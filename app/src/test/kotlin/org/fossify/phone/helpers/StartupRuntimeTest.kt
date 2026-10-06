package org.fossify.phone.classic.helpers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import org.fossify.phone.classic.activities.MainActivity
import org.fossify.phone.classic.activities.SplashActivity
import org.fossify.phone.classic.activities.DialpadActivity
import org.fossify.phone.classic.extensions.getRaabtaContactsCursor
import org.robolectric.RuntimeEnvironment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StartupRuntimeTest {
    @Test fun phoneMainCreatesAndResumes() {
        Robolectric.buildActivity(MainActivity::class.java).use { it.setup() }
    }

    @Test fun launcherSplashCreates() {
        Robolectric.buildActivity(SplashActivity::class.java).use { it.create() }
    }

    @Test fun dialpadCreatesAndResumes() {
        Robolectric.buildActivity(DialpadActivity::class.java).use { it.setup() }
    }

    @Test(expected = IllegalStateException::class)
    fun privateProviderRejectsUiThreadQueriesAtTheBoundary() {
        RuntimeEnvironment.getApplication().getRaabtaContactsCursor(false, true)
    }

    @Test fun combinedLoaderQueriesOffUiThreadAndClosesCursorBeforeDelivering() {
        val app = RuntimeEnvironment.getApplication()
        val queried = CountDownLatch(1)
        val cursor = MatrixCursor(arrayOf("raw_id", "contact_id", "name", "photo_uri", "phone_numbers", "birthdays", "anniversaries"))
        cursor.addRow(arrayOf<Any>(1000001, 1000001, "Test Private Contact", "", "[]", "[]", "[]"))
        var queryOnMain = true
        var delivered = false
        ShadowContentResolver.registerProviderInternal("${app.packageName}.privatecontacts", object : ContentProvider() {
            override fun onCreate() = true
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?) = cursor.also {
                queryOnMain = Looper.myLooper() == Looper.getMainLooper()
                queried.countDown()
            }
            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
        })
        ContactsCache.loadWithPrivate(app) {
            assertSame(Looper.getMainLooper(), Looper.myLooper())
            assertTrue(cursor.isClosed)
            assertTrue("Rebranded decoder dropped the private contact", it.any { contact -> contact.getNameToDisplay() == "Test Private Contact" })
            delivered = true
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Private query did not run", queried.await(5, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!delivered && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertFalse("Private query ran on the UI thread", queryOnMain)
        assertTrue("Contacts were not delivered", delivered)
    }
}
