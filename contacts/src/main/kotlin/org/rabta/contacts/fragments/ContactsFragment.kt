package org.rabta.contacts.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.models.contacts.Contact
import org.rabta.contacts.activities.EditContactActivity
import org.rabta.contacts.activities.InsertOrEditContactActivity
import org.rabta.contacts.activities.MainActivity
import org.rabta.contacts.activities.SimpleActivity
import org.rabta.contacts.adapters.ContactsAdapter
import org.rabta.contacts.databinding.RcFragmentContactsBinding
import org.rabta.contacts.databinding.RcFragmentLettersLayoutBinding
import org.rabta.contacts.extensions.config
import org.rabta.contacts.extensions.viewContact
import org.rabta.contacts.helpers.LOCATION_CONTACTS_TAB
import org.rabta.contacts.interfaces.RefreshContactsListener

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LetterLayout>(context, attributeSet) {

    private lateinit var binding: RcFragmentContactsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = RcFragmentContactsBinding.bind(this)
        innerBinding = LetterLayout(RcFragmentLettersLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        activity?.hideKeyboard()
        Intent(context, EditContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun placeholderClicked() {
        if (activity is MainActivity) {
            (activity as MainActivity).showFilterDialog()
        } else if (activity is InsertOrEditContactActivity) {
            (activity as InsertOrEditContactActivity).showFilterDialog()
        }
    }

    fun setupContactsAdapter(contacts: List<Contact>) {
        setupViewVisibility(contacts.isNotEmpty())
        val currAdapter = innerBinding.fragmentList.adapter

        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = LOCATION_CONTACTS_TAB

            ContactsAdapter(
                activity = activity as SimpleActivity,
                contactItems = contacts.toMutableList(),
                refreshListener = activity as RefreshContactsListener,
                location = location,
                removeListener = null,
                recyclerView = innerBinding.fragmentList,
                enableDrag = false,
                itemClick = {
                    (activity as RefreshContactsListener).contactClicked(it as Contact)
                },
                profileIconClick = {
                    activity?.viewContact(it as Contact)
                }
            ).apply {
                innerBinding.fragmentList.adapter = this
            }

            if (context.areSystemAnimationsEnabled) {
                innerBinding.fragmentList.scheduleLayoutAnimation()
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = context.config.startNameWithSurname
                showPhoneNumbers = context.config.showPhoneNumbers
                showContactThumbnails = context.config.showContactThumbnails
                updateItems(contacts)
            }
        }
    }
}
