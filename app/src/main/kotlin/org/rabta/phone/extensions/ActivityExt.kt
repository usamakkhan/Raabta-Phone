package org.rabta.phone.classic.extensions

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.provider.ContactsContract
import org.fossify.commons.helpers.CONTACT_ID
import org.fossify.commons.helpers.FIRST_CONTACT_ID
import org.fossify.commons.helpers.IS_PRIVATE
import org.fossify.commons.helpers.ON_CLICK_CALL_CONTACT
import org.fossify.commons.helpers.ON_CLICK_VIEW_CONTACT
import org.fossify.commons.models.contacts.Contact
import org.rabta.phone.classic.activities.SimpleActivity

fun SimpleActivity.handleGenericContactClick(contact: Contact) {
    if (contact.phoneNumbers.isEmpty()) { startContactDetailsIntent(contact); return }
    when (config.onContactClick) {
        ON_CLICK_CALL_CONTACT -> startCallWithConfirmationCheck(contact)
        ON_CLICK_VIEW_CONTACT -> startContactDetailsIntent(contact)
    }
}

fun SimpleActivity.launchCreateNewContactIntent() {
    startActivity(Intent(this, org.rabta.contacts.activities.EditContactActivity::class.java)
        .setAction(Intent.ACTION_INSERT).setData(ContactsContract.Contacts.CONTENT_URI))
}

fun Activity.startContactDetailsIntent(contact: Contact) {
    val request = Intent(this, org.rabta.contacts.activities.ViewContactActivity::class.java).setAction(Intent.ACTION_VIEW)
    if (contact.rawId >= FIRST_CONTACT_ID && contact.rawId == contact.contactId) {
        request.putExtra(CONTACT_ID, contact.rawId).putExtra(IS_PRIVATE, true)
    } else {
        request.data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toLong())
    }
    startActivity(request)
}

/** Compatibility route for the retained, non-exported legacy screen. */
fun Activity.launchOtherContactApp(request: Intent) {
    val activity = when (request.action) {
        Intent.ACTION_INSERT, Intent.ACTION_EDIT -> org.rabta.contacts.activities.EditContactActivity::class.java
        Intent.ACTION_INSERT_OR_EDIT -> org.rabta.contacts.activities.InsertOrEditContactActivity::class.java
        else -> org.rabta.contacts.activities.ViewContactActivity::class.java
    }
    startActivity(Intent(request).setClass(this, activity))
}
