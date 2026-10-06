package org.rabta.phone.classic.helpers

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

/** Source/manifest contracts, not a substitute for device telephony/provider tests. */
class PlatformRoutingContractTest {
    private val app = File(requireNotNull(System.getProperty("raabta.app.dir")))
    private val kotlin = File(app, "src/main/kotlin")
    private fun source(path: String) = File(kotlin, "org/rabta/phone/$path").readText().replace("\r\n", "\n")

    @Test fun noCallPathUsesTheUpstreamPackageBoundLauncher() {
        val legacy = Regex("\\blaunchCallIntent\\s*\\(")
        kotlin.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            assertFalse("Upstream call launcher in ${file.name}", legacy.containsMatchIn(file.readText()))
            assertFalse(file.readText().contains("org.fossify.commons.extensions.launchCallIntent"))
        }
        assertEquals(5, Regex("launchRaabtaCallIntent\\(").findAll(source("extensions/CallExt.kt")).count())
    }

    @Test fun permissionBranchesTargetRegisteredAppClasses() {
        val launch = source("extensions/CallLaunchExt.kt")
        assertTrue(launch.contains("handlePermission(PERMISSION_CALL_PHONE)"))
        assertTrue(launch.contains("Intent(this, DialerActivity::class.java).setAction(Intent.ACTION_CALL)"))
        assertTrue(launch.contains("Intent(this, DialpadActivity::class.java).setAction(Intent.ACTION_DIAL)"))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File(app, "src/main/AndroidManifest.xml"))
        val activities = manifest.getElementsByTagName("activity")
        val ns = "http://schemas.android.com/apk/res/android"
        for ((activityName, action) in listOf("DialerActivity" to "android.intent.action.CALL", "DialpadActivity" to "android.intent.action.DIAL")) {
            val element = (0 until activities.length).map { activities.item(it) as Element }.single {
                it.getAttributeNS(ns, "name") == ".activities.$activityName"
            }
            assertEquals("true", element.getAttributeNS(ns, "exported"))
            if (activityName == "DialerActivity") {
                assertEquals("android.permission.CALL_PHONE", element.getAttributeNS(ns, "permission"))
            } else {
                assertEquals("", element.getAttributeNS(ns, "permission")) // Opening tel: links must remain available.
            }
            val actions = element.getElementsByTagName("action")
            assertTrue((0 until actions.length).any { (actions.item(it) as Element).getAttributeNS(ns, "name") == action })
            assertTrue(source("activities/$activityName.kt").startsWith("package org.rabta.phone.classic.activities"))
        }
    }

    @Test fun rawContactReadDoesNotProjectAnOptionalColumn() {
        val store = source("helpers/ContactStore.kt")
        assertTrue(store.contains("resolver.query(RawContacts.CONTENT_URI, null,"))
        assertTrue(store.contains("getColumnIndex(RawContacts.RAW_CONTACT_IS_READ_ONLY)"))
        assertFalse(store.contains("getColumnIndexOrThrow(RawContacts.RAW_CONTACT_IS_READ_ONLY)"))
        assertTrue(store.contains("readOnlyIndex >= 0"))
    }

    @Test fun asynchronousContactLoadingCannotReplaceDialpadInput() {
        val dialpad = source("activities/DialpadActivity.kt")
        val receive = dialpad.substringAfter("private fun gotContacts(").substringBefore("private fun dialpadValueChanged(")
        assertFalse(receive.contains("checkDialIntent("))
        assertFalse(receive.contains("setText("))
        assertTrue(dialpad.contains("if (savedInstanceState == null) checkDialIntent()"))
        assertTrue(dialpad.contains("intent.data?.scheme == \"tel\""))
    }

    @Test fun textChangesCannotExecuteSpecialDialerCodes() {
        val dialpad = source("activities/DialpadActivity.kt")
        val watcher = dialpad.substringAfter("private fun dialpadValueChanged(").substringBefore("private fun initCall(")
        assertFalse(watcher.contains("sendDialerSpecialCode"))
        assertFalse(watcher.contains("sendBroadcast"))
        assertFalse(watcher.contains("runSecretCodeFromUserAction"))
        assertEquals(2, Regex("if \\(runSecretCodeFromUserAction\\(number\\)\\)").findAll(dialpad).count())
    }

    @Test fun restoredContactDraftDoesNotReapplyOriginalPhoneExtra() {
        val contact = source("activities/ContactActivity.kt")
        assertTrue(contact.contains("val restoringDraft = restoredDraft != null"))
        assertTrue(contact.contains("if (!restoringDraft && record != null && extraPhone.isNotBlank()"))
        assertTrue(contact.contains("showDetails()\n                if (intent.action == Intent.ACTION_EDIT) chooseSource()"))
    }

    @Test fun simExtrasAreTypedAndNeverForceUnwrapped() {
        val calls = source("extensions/CallExt.kt")
        assertTrue(calls.contains("IntentCompat.getParcelableExtra"))
        assertFalse(calls.contains("!!"))
        assertTrue(calls.contains("CallAccountPolicy.select("))
        val simAction = calls.substringAfter("fun BaseSimpleActivity.callContactWithSim(")
            .substringBefore("fun BaseSimpleActivity.callContactWithSimWithConfirmationCheck(")
        assertTrue(simAction.indexOf("if (!granted) return@handlePermission") < simAction.indexOf("getAvailableSIMCardLabels()"))
        assertTrue(simAction.contains("if (handle == null) toast("))
    }

    @Test fun delayedDialpadUpdatesPreserveNewTyping() {
        val dialpad = source("activities/DialpadActivity.kt")
        assertTrue(dialpad.contains("inputRevision++; dialpadValueChanged(it)"))
        assertTrue(dialpad.contains("if (inputRevision == revision) clearInput()"))
        assertTrue(dialpad.contains("inputRevision == revision && binding.dialpadInput.value.isEmpty()"))
    }

    @Test fun dialpadReleasesNativeAudioAndCursorOnDestroy() {
        val dialpad = source("activities/DialpadActivity.kt")
        val destroy = dialpad.substringAfter("override fun onDestroy()").substringBefore("private fun setupOptionsMenu")
        assertTrue(destroy.contains("toneGeneratorHelper?.release()"))
        assertFalse(dialpad.contains("private var privateCursor"))
        assertTrue(dialpad.contains("ContactsCache.loadWithPrivate(this)"))
        assertTrue(source("helpers/ContactsCache.kt").contains(".use { cursor ->"))
        val tone = source("helpers/ToneGeneratorHelper.kt")
        assertTrue(tone.contains("toneGenerator?.release()"))
        assertTrue(tone.contains("handler.removeCallbacks(stopPendingTone)"))
    }
}
