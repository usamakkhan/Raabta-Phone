package org.fossify.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import org.fossify.commons.helpers.TAB_GROUPS
import org.fossify.contacts.activities.MainActivity
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.databinding.RcFragmentGroupsBinding
import org.fossify.contacts.databinding.RcFragmentLayoutBinding
import org.fossify.contacts.dialogs.CreateNewGroupDialog

class GroupsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.FragmentLayout>(context, attributeSet) {

    private lateinit var binding: RcFragmentGroupsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = RcFragmentGroupsBinding.bind(this)
        innerBinding = FragmentLayout(RcFragmentLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        finishActMode()
        showNewGroupsDialog()
    }

    override fun placeholderClicked() {
        showNewGroupsDialog()
    }

    private fun showNewGroupsDialog() {
        CreateNewGroupDialog(activity as SimpleActivity) {
            (activity as? MainActivity)?.refreshContacts(TAB_GROUPS)
        }
    }
}
