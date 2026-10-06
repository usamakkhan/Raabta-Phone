package org.rabta.phone.classic.activities

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.OnBackPressedCallback
import org.rabta.phone.classic.BuildConfig

class CreditsActivity : RaabtaScreenActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (toolbar.title != "Credits") showCredits() else finish()
            }
        })
        showCredits()
    }

    private fun showCredits() {
        body.removeAllViews()
        toolbar.title = "Credits"
        label("Raabta Phone", true)
        label("Created and customized by\nUsama", true)
        label("developed with ❤️ by Usama\nVersion ${BuildConfig.VERSION_NAME}")
        label("A simpler way to find your people. Calling and contacts, together.")
        action("Developer email: user01usama@gmail.com") {
            try {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "user01usama@gmail.com", null))
                    .putExtra(Intent.EXTRA_SUBJECT, "Raabta Phone ${BuildConfig.VERSION_NAME}"))
            } catch (_: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(this, "Email user01usama@gmail.com using your preferred email app.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        action("Open-source licenses") {
            body.removeAllViews()
            toolbar.title = "Open-source licenses"
            label("Legal notices", true)
            label("Fossify Phone 1.11.1, Fossify Contacts 1.6.0 and Fossify Commons 6.1.6 — original code, interfaces, assets and contributors. GPL-3.0.\n\nThis is an independent modified edition. Usama is credited for this edition, not sole authorship of its upstream components.")
            label("Third-party libraries include AndroidX and Material Components (Apache-2.0), Kotlin and coroutines (Apache-2.0), Glide (BSD/Apache-2.0), libphonenumber (Apache-2.0), EventBus (Apache-2.0), IndicatorFastScroll (Apache-2.0), and AutofitTextView (Apache-2.0).")
            action("GNU GPL version 3 — full license") {
                body.removeAllViews()
                toolbar.title = "GNU GPL version 3"
                label(assets.open("COPYING.txt").bufferedReader().use { it.readText() }).setTextIsSelectable(true)
            }
        }
    }
}
