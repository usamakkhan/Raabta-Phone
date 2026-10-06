package org.fossify.phone.classic.helpers

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CallContactThreadContractTest {
    private val root = File(requireNotNull(System.getProperty("raabta.app.dir")))

    @Test fun privateLookupHappensBeforeMainThreadContactsCallback() {
        val source = File(root, "src/main/kotlin/org/fossify/phone/helpers/CallContactHelper.kt").readText()
        val query = source.indexOf("val privateContacts = context.getMyContactsCursor")
        val callback = source.indexOf("ContactsHelper(context).getContacts")
        assertTrue("Private contact lookup must precede the main-thread callback", query >= 0 && query < callback)
    }

    @Test fun secondaryPrivateContactScreensUseWorkerQueries() {
        val filter = File(root, "src/main/kotlin/org/fossify/phone/dialogs/FilterContactSourcesDialog.kt").readText()
        val speedDial = File(root, "src/main/kotlin/org/fossify/phone/activities/ManageSpeedDialActivity.kt").readText()
        val filterWorker = filter.indexOf("ensureBackgroundThread {")
        val filterQuery = filter.indexOf("activity.getMyContactsCursor")
        val speedWorker = speedDial.indexOf("ensureBackgroundThread {")
        val speedQuery = speedDial.indexOf("getMyContactsCursor(favoritesOnly")
        assertTrue("Filter sources must dispatch private provider work off main", filterWorker >= 0 && filterWorker < filterQuery)
        assertTrue("Speed dial must dispatch private provider work off main", speedWorker >= 0 && speedWorker < speedQuery)
    }

    @Test fun recentPrivateCursorIsClosed() {
        val source = File(root, "src/main/kotlin/org/fossify/phone/fragments/RecentsFragment.kt").readText()
        val method = source.substringAfter("private fun getPrivateContacts()").substringBefore("\n    }")
        assertTrue("Recent private contact cursor must be closed", method.contains(".use"))
    }
}
