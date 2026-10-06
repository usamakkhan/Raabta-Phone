package org.rabta.contacts.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import org.fossify.commons.helpers.SHOW_EMAILS_FIELD
import org.fossify.commons.helpers.SHOW_TABS
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_FAVORITES
import org.fossify.commons.helpers.TAB_GROUPS

class Config(context: Context) : BaseConfig(context) {
    init {
        migrateTabDefaults()
        migrateEmailVisibilityDefault()
    }

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showTabs: Int
        get() = prefs.getInt("raabta_contacts_show_tabs", DEFAULT_VISIBLE_TABS_MASK)
        set(showTabs) = prefs.edit().putInt("raabta_contacts_show_tabs", showTabs).apply()

    var tabOrder: List<Int>
        get() = prefs.getString("raabta_contacts_tab_order", null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { tabsList.contains(it) }
            ?.distinct()
            ?: tabsList
        set(value) = prefs.edit().putString("raabta_contacts_tab_order", value.joinToString(",")).apply()

    val orderedTabs: List<Int>
        get() = (tabOrder + tabsList).distinct().filter { tabsList.contains(it) }

    var classicSearch: Boolean
        get() = prefs.getBoolean("classic_prefix_search", true)
        set(value) = prefs.edit().putBoolean("classic_prefix_search", value).apply()

    var contactsOnContactClick: Int
        get() = prefs.getInt("raabta_contacts_on_click", org.fossify.commons.helpers.ON_CLICK_VIEW_CONTACT)
        set(value) = prefs.edit().putInt("raabta_contacts_on_click", value).apply()

    var contactsLastTab: Int
        get() = prefs.getInt("raabta_contacts_last_tab", 0)
        set(value) = prefs.edit().putInt("raabta_contacts_last_tab", value).apply()

    var contactsDefaultTab: Int
        get() = prefs.getInt("raabta_contacts_default_tab", org.rabta.contacts.helpers.TAB_KEYPAD)
        set(value) = prefs.edit().putInt("raabta_contacts_default_tab", value).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactSources) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactSources)
            .apply()

    private fun migrateTabDefaults() {
        val migratedKey = "raabta_contacts_tabs_v2_migrated"
        if (prefs.getBoolean(migratedKey, false)) {
            return
        }

        val legacyAllTabs = TAB_CONTACTS or TAB_FAVORITES or TAB_GROUPS
        val editor = prefs.edit().putBoolean(migratedKey, true)
        if (prefs.getInt("raabta_contacts_show_tabs", legacyAllTabs) == legacyAllTabs) {
            editor.putInt("raabta_contacts_show_tabs", DEFAULT_VISIBLE_TABS_MASK)
                .putInt("raabta_contacts_default_tab", TAB_KEYPAD)
        }
        editor.apply()
    }

    private fun migrateEmailVisibilityDefault() {
        val migratedKey = "raabta_contacts_email_visibility_default_v1"
        if (prefs.getBoolean(migratedKey, false)) {
            return
        }

        // Details and editor use the same visible-fields mask. Keep the stored email data,
        // but start with email rows hidden; Manage visible fields can turn them back on.
        showContactFields = showContactFields and SHOW_EMAILS_FIELD.inv()
        prefs.edit().putBoolean(migratedKey, true).apply()
    }

}
