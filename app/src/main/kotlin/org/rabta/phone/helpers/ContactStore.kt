package org.rabta.phone.classic.helpers

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

/** Edits only explicitly chosen fields on a chosen writable raw contact. */
class ContactStore(private val resolver: ContentResolver) {
    data class Source(val id: Long, val label: String, val writable: Boolean)
    data class Field(val id: Long, val raw: Long, val kind: String, val value: String, val type: Int)
    data class Record(val id: Long, val name: String, val lookup: String, val photo: String?, val starred: Boolean,
                      val sources: List<Source>, val fields: List<Field>)

    fun resolve(uri: Uri): Long {
        require(uri.scheme == "content" && uri.authority == ContactsContract.AUTHORITY) { "Unsupported contact link" }
        if (uri.pathSegments.firstOrNull() == "raw_contacts") {
            return resolver.query(uri, arrayOf(RawContacts.CONTACT_ID), null, null, null)?.use {
                check(it.moveToFirst()) { "Contact not found" }; it.getLong(0)
            } ?: error("Contact not found")
        }
        val direct = ContactsContract.Contacts.lookupContact(resolver, uri) ?: uri
        return resolver.query(direct, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use {
            check(it.moveToFirst()) { "Contact not found" }; it.getLong(0)
        } ?: error("Contact not found")
    }

    fun read(id: Long): Record {
        var name = ""; var lookup = ""; var photo: String? = null; var starred = false
        resolver.query(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI, ContactsContract.Contacts.STARRED), null, null, null)?.use {
            check(it.moveToFirst()) { "Contact no longer exists" }
            name = it.getString(0).orEmpty(); lookup = it.getString(1).orEmpty()
            photo = it.getString(2); starred = it.getInt(3) == 1
        } ?: error("Contacts are unavailable")
        val sources = mutableListOf<Source>()
        val syncAdapters by lazy {
            try { ContentResolver.getSyncAdapterTypes().filter { it.authority == ContactsContract.AUTHORITY } }
            catch (_: SecurityException) { emptyList() }
        }
        // Some Android 13 providers reject RAW_CONTACT_IS_READ_ONLY in projections.
        // Ask for the provider's supported columns, then treat this flag as optional.
        resolver.query(RawContacts.CONTENT_URI, null,
            "${RawContacts.CONTACT_ID}=? AND ${RawContacts.DELETED}=0", arrayOf(id.toString()), null)?.use {
            val rawIndex = it.getColumnIndexOrThrow(RawContacts._ID)
            val nameIndex = it.getColumnIndexOrThrow(RawContacts.ACCOUNT_NAME)
            val typeIndex = it.getColumnIndexOrThrow(RawContacts.ACCOUNT_TYPE)
            val readOnlyIndex = it.getColumnIndex(RawContacts.RAW_CONTACT_IS_READ_ONLY)
            while (it.moveToNext()) {
                val accountType = it.getString(typeIndex)
                val readOnly = if (readOnlyIndex >= 0 && !it.isNull(readOnlyIndex)) it.getInt(readOnlyIndex) != 0 else null
                val supportsUploading = if (readOnly == null && !accountType.isNullOrBlank())
                    syncAdapters.firstOrNull { adapter -> adapter.accountType == accountType }?.supportsUploading() else null
                sources.add(Source(it.getLong(rawIndex), it.getString(nameIndex) ?: "On this device",
                    ContactAccountPolicy.canEdit(accountType, readOnly, supportsUploading)))
            }
        }
        val fields = mutableListOf<Field>()
        resolver.query(Data.CONTENT_URI, arrayOf(Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2),
            "${Data.CONTACT_ID}=?", arrayOf(id.toString()), null)?.use {
            while (it.moveToNext()) {
                val kind = it.getString(2)
                if (kind in listOf(StructuredName.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, Note.CONTENT_ITEM_TYPE)) {
                    fields.add(Field(it.getLong(0), it.getLong(1), kind, it.getString(3).orEmpty(),
                        if (kind == Phone.CONTENT_ITEM_TYPE || kind == Email.CONTENT_ITEM_TYPE) it.getInt(4) else 0))
                }
            }
        }
        return Record(id, name, lookup, photo, starred, sources, fields)
    }

    fun save(original: Record?, raw: Long?, changed: List<Field>): Long {
        if (original != null) {
            val fresh = read(original.id)
            check(fresh.sources.any { it.id == raw && it.writable }) { "This contact account is read-only" }
            check(fresh.fields.filter { it.raw == raw }.sortedBy { it.id } == original.fields.filter { it.raw == raw }.sortedBy { it.id }) {
                "This contact changed elsewhere. Reopen it before saving."
            }
        }
        require(changed.any { it.value.isNotBlank() }) { "Enter a name, number or email" }
        val batch = arrayListOf<ContentProviderOperation>()
        if (original == null) batch.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            .withValue(RawContacts.ACCOUNT_TYPE, null).withValue(RawContacts.ACCOUNT_NAME, null).build())
        val old = original?.fields?.filter { it.raw == raw }.orEmpty()
        val plan = ContactEditPlan.create(old, raw, changed)
        plan.deleted.forEach {
            batch.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection("${Data._ID}=? AND ${Data.RAW_CONTACT_ID}=?", arrayOf(it.id.toString(), raw.toString()))
                .withExpectedCount(1).build())
        }
        plan.changed.forEach { field ->
            require(field.kind in listOf(StructuredName.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, Note.CONTENT_ITEM_TYPE))
            if (field.id > 0) {
                check(old.any { it.id == field.id && it.kind == field.kind })
                if (old.first { it.id == field.id }.value != field.value) {
                    batch.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection("${Data._ID}=? AND ${Data.RAW_CONTACT_ID}=?", arrayOf(field.id.toString(), raw.toString()))
                        .withValue(Data.DATA1, field.value).withExpectedCount(1).build())
                }
            } else {
                val op = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.MIMETYPE, field.kind).withValue(Data.DATA1, field.value)
                if (field.kind == Phone.CONTENT_ITEM_TYPE || field.kind == Email.CONTENT_ITEM_TYPE) op.withValue(Data.DATA2, field.type)
                if (original == null) op.withValueBackReference(Data.RAW_CONTACT_ID, 0) else op.withValue(Data.RAW_CONTACT_ID, raw)
                batch.add(op.build())
            }
        }
        if (batch.isEmpty()) return original!!.id
        val results = resolver.applyBatch(ContactsContract.AUTHORITY, batch)
        ContactsCache.invalidate()
        if (original != null) return original.id
        val createdRaw = results.first().uri ?: error("Contact could not be created")
        return resolve(createdRaw)
    }

    fun star(record: Record) {
        resolver.update(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, record.id),
            ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (record.starred) 0 else 1) }, null, null)
        ContactsCache.invalidate()
    }
    fun delete(record: Record) {
        val fresh = read(record.id)
        check(fresh.sources.all { it.writable }) { "This contact includes a read-only account" }
        resolver.delete(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, record.id), null, null)
        ContactsCache.invalidate()
    }
}
