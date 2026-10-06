package org.fossify.phone.classic.helpers

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.ContactsContract
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.phone.classic.helpers.PrivateContactsReader as MyContactsContentProvider
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.contacts.Contact
import org.fossify.phone.classic.extensions.getRaabtaContactsCursor

/** Shares in-flight provider reads between tabs; provider changes invalidate the short cache. */
object ContactsCache {
    private val main = Handler(Looper.getMainLooper())
    private val cached = mutableMapOf<Pair<Boolean, Boolean>, Pair<Long, List<Contact>>>()
    private val waiting = mutableMapOf<Pair<Boolean, Boolean>, MutableList<(ArrayList<Contact>) -> Unit>>()
    private var observing = false
    private var generation = 0
    @Synchronized fun invalidate() { cached.clear(); generation++ }

    /** In-process providers execute on the caller's thread, unlike a separate Contacts app. */
    fun loadWithPrivate(context: Context, all: Boolean = false, numbersOnly: Boolean = true,
                        includePrivate: Boolean = true, privateFavoritesOnly: Boolean = false,
                        callback: (ArrayList<Contact>) -> Unit) {
        val app = context.applicationContext
        load(app, all, numbersOnly) { contacts ->
            ensureBackgroundThread {
                if (includePrivate) {
                    val privateContacts = app.getRaabtaContactsCursor(privateFavoritesOnly, numbersOnly).use { cursor ->
                        MyContactsContentProvider.getContacts(app, cursor)
                    }
                    contacts.addAll(if (privateFavoritesOnly) privateContacts.map { it.copy(starred = 1) } else privateContacts)
                    if (privateContacts.isNotEmpty()) contacts.sort()
                }
                main.post { callback(contacts) }
            }
        }
    }
    @Synchronized fun load(context: Context, all: Boolean = false, numbersOnly: Boolean = true,
                           callback: (ArrayList<Contact>) -> Unit) {
        val app = context.applicationContext
        if (app.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            invalidate(); main.post { callback(arrayListOf()) }; return
        }
        if (!observing) {
            app.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true,
                object : ContentObserver(main) { override fun onChange(selfChange: Boolean) { invalidate() } })
            observing = true
        }
        val key = all to numbersOnly
        cached[key]?.let { (time, items) ->
            if (SystemClock.elapsedRealtime() - time < 2000) {
                main.post { callback(ArrayList(items)) }; return
            }
        }
        waiting[key]?.let { it.add(callback); return }
        waiting[key] = mutableListOf(callback)
        val startedGeneration = generation
        ContactsHelper(app).getContacts(getAll = all, showOnlyContactsWithNumbers = numbersOnly) { items ->
            val callbacks = synchronized(this) {
                if (startedGeneration == generation) cached[key] = SystemClock.elapsedRealtime() to items.toList()
                waiting.remove(key).orEmpty()
            }
            main.post {
                if (startedGeneration != synchronized(this) { generation }) {
                    callbacks.forEach { load(app, all, numbersOnly, it) }
                } else callbacks.forEach { it(ArrayList(items)) }
            }
        }
    }
}
