package org.fossify.phone.classic.helpers

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

/** Build/source contracts only; actual combined UI/provider behavior needs a device. */
class FullIntegrationContractTest {
    private val app = File(requireNotNull(System.getProperty("raabta.app.dir")))
    private val contacts = File(app.parentFile, "contacts")
    private fun source(path: String) = File(app, "src/main/kotlin/org/fossify/phone/$path").readText()
    private fun contactSource(path: String) = File(contacts, "src/main/kotlin/org/fossify/contacts/$path").readText()
    private val ns = "http://schemas.android.com/apk/res/android"
    private fun activities(module: File): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(File(module, "src/main/AndroidManifest.xml"))
        val nodes = doc.getElementsByTagName("activity")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }
    @Test fun completeContactsActivitiesRemainRegistered() {
        val names = activities(contacts).map { it.getAttributeNS(ns, "name") }
        for (name in listOf("MainActivity", "SettingsActivity", "ViewContactActivity", "EditContactActivity", "GroupContactsActivity", "InsertOrEditContactActivity")) {
            assertTrue(name, names.contains(".activities.$name"))
        }
    }
    @Test fun legacyEditorDoesNotCompeteForExternalContactIntents() {
        val legacy = activities(app).single { it.getAttributeNS(ns, "name") == ".activities.ContactActivity" }
        assertEquals("false", legacy.getAttributeNS(ns, "exported"))
        assertEquals(0, legacy.getElementsByTagName("intent-filter").length)
    }
    @Test fun phoneActionsUseFullContactsClasses() {
        val routes = source("extensions/ActivityExt.kt")
        assertTrue(routes.contains("org.fossify.contacts.activities.ViewContactActivity::class.java"))
        assertTrue(routes.contains("org.fossify.contacts.activities.EditContactActivity::class.java"))
        assertTrue(source("extensions/Context.kt").contains("org.fossify.contacts.activities.InsertOrEditContactActivity::class.java"))
        assertTrue(source("activities/MainActivity.kt").contains("R.id.full_contacts -> startActivity"))
    }
    @Test fun contactsCallsUseTheCombinedPhonePackage() {
        val calls = contactSource("extensions/Activity.kt")
        assertTrue(calls.contains("setClassName(packageName, if (it) \"org.fossify.phone.classic.activities.DialerActivity\""))
        assertTrue(calls.contains("handlePermission(PERMISSION_CALL_PHONE)"))
    }
    @Test fun contactsLayoutsCannotOverridePhoneLayouts() {
        val phoneLayouts = File(app, "src/main/res/layout").listFiles()!!.map { it.name }.toSet()
        val contactLayouts = File(contacts, "src/main/res/layout").listFiles()!!.map { it.name }.toSet()
        assertEquals(51, contactLayouts.size)
        assertTrue(contactLayouts.all { it.startsWith("rc_") })
        assertTrue(phoneLayouts.intersect(contactLayouts).isEmpty())
    }
    @Test fun contactTabPreferencesAreSeparateButPrefixSearchIsShared() {
        val config = contactSource("helpers/Config.kt")
        assertTrue(config.contains("raabta_contacts_show_tabs"))
        assertTrue(config.contains("raabta_contacts_last_tab"))
        assertTrue(config.contains("raabta_contacts_default_tab"))
        assertTrue(config.contains("raabta_contacts_on_click"))
        assertTrue(config.contains("classic_prefix_search"))
        assertFalse(contactSource("activities/MainActivity.kt").contains("config.lastUsedViewPagerPage"))
        assertFalse(contactSource("activities/MainActivity.kt").contains("config.defaultTab"))
    }
    @Test fun fullVariantDisablesShrinkingAndIncludesContactsModule() {
        val build = File(app, "build.gradle.kts").readText()
        val full = build.substringAfter("buildTypes.create(\"full\")").substringBefore("sourceSets")
        assertTrue(full.contains("isMinifyEnabled = false"))
        assertTrue(full.contains("isShrinkResources = false"))
        assertTrue(full.contains("isDebuggable = false"))
        assertTrue(build.contains("implementation(project(\":contacts\"))"))
    }
}
