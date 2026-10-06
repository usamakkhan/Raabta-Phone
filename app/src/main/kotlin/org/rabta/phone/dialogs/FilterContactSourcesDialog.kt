package org.rabta.phone.classic.dialogs
import org.rabta.phone.classic.extensions.getRaabtaContactsCursor as getMyContactsCursor

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.rabta.phone.classic.helpers.PrivateContactsReader as MyContactsContentProvider
import org.fossify.commons.helpers.SMT_PRIVATE
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource
import org.rabta.phone.classic.R
import org.rabta.phone.classic.activities.SimpleActivity
import org.rabta.phone.classic.adapters.FilterContactSourcesAdapter
import org.rabta.phone.classic.databinding.DialogFilterContactSourcesBinding
import org.rabta.phone.classic.extensions.config

class FilterContactSourcesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private val binding by activity.viewBinding(DialogFilterContactSourcesBinding::inflate)

    private var dialog: AlertDialog? = null
    private var contactSources = ArrayList<ContactSource>()
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        val contactHelper = ContactsHelper(activity)
        contactHelper.getContactSources { contactSources ->
            contactSources.mapTo(this@FilterContactSourcesDialog.contactSources) { it.copy() }
            isContactSourcesReady = true
            processDataIfReady()
        }

        contactHelper.getContacts(getAll = true, showOnlyContactsWithNumbers = true) { publicContacts ->
            // Commons delivers this callback on the main thread. The combined
            // app's private Room provider must only be queried from a worker.
            ensureBackgroundThread {
                val privateCursor = activity.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                val privateContacts = MyContactsContentProvider.getContacts(activity, privateCursor)
                activity.runOnUiThread {
                    publicContacts.mapTo(contacts) { contact -> contact.copy() }
                    contacts.addAll(privateContacts)
                    isContactsReady = true
                    processDataIfReady()
                }
            }
        }
    }

    private fun processDataIfReady() {
        if (!isContactSourcesReady) {
            return
        }

        val contactSourcesWithCount = ArrayList<ContactSource>()
        for (contactSource in contactSources) {
            val count = if (isContactsReady) {
                contacts.count { it.source == contactSource.name }
            } else {
                -1
            }
            contactSourcesWithCount.add(contactSource.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            val selectedSources = activity.getVisibleContactSources()
            binding.filterContactSourcesList.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedSources)

            if (dialog == null) {
                activity.getAlertDialogBuilder()
                    .setPositiveButton(R.string.ok) { _, _ -> confirmContactSources() }
                    .setNegativeButton(R.string.cancel, null)
                    .apply {
                        activity.setupDialogStuff(binding.root, this) { alertDialog ->
                            dialog = alertDialog
                        }
                    }
            }
        }
    }

    private fun confirmContactSources() {
        val selectedContactSources = (binding.filterContactSourcesList.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
        val ignoredContactSources = contactSources.filter { !selectedContactSources.contains(it) }.map {
            if (it.type == SMT_PRIVATE) SMT_PRIVATE else it.getFullIdentifier()
        }.toHashSet()

        if (activity.getVisibleContactSources() != ignoredContactSources) {
            activity.config.ignoredContactSources = ignoredContactSources
            callback()
        }
        dialog?.dismiss()
    }
}
