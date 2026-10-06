package org.rabta.contacts.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_FAVORITES
import org.fossify.commons.helpers.TAB_GROUPS
import org.fossify.commons.views.MyAppCompatCheckbox
import org.rabta.contacts.R
import org.rabta.contacts.extensions.config
import org.rabta.contacts.helpers.DEFAULT_VISIBLE_TABS_MASK
import org.rabta.contacts.helpers.TAB_KEYPAD
import org.rabta.contacts.helpers.TAB_RECENTS

class ManageVisibleTabsDialog(val activity: BaseSimpleActivity) {
    private var view = activity.layoutInflater.inflate(R.layout.rc_dialog_manage_visible_tabs, null)
    private val tabs = LinkedHashMap<Int, Int>()

    init {
        tabs.apply {
            put(TAB_KEYPAD, R.id.manage_visible_tabs_keypad)
            put(TAB_RECENTS, R.id.manage_visible_tabs_recents)
            put(TAB_CONTACTS, R.id.manage_visible_tabs_contacts)
            put(TAB_FAVORITES, R.id.manage_visible_tabs_favorites)
            put(TAB_GROUPS, R.id.manage_visible_tabs_groups)
        }

        val showTabs = activity.config.showTabs
        for ((key, value) in tabs) {
            view.findViewById<MyAppCompatCheckbox>(value).isChecked = showTabs and key != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in tabs) {
            if (view.findViewById<MyAppCompatCheckbox>(value).isChecked) {
                result += key
            }
        }

        if (result == 0) {
            result = DEFAULT_VISIBLE_TABS_MASK
        }

        activity.config.showTabs = result
    }
}
