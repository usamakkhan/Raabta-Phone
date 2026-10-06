package org.fossify.phone.classic.extensions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.PERMISSION_CALL_PHONE
import org.fossify.phone.classic.activities.DialerActivity
import org.fossify.phone.classic.activities.DialpadActivity

/** Use actual app classes: Commons' launcher hard-codes the upstream package. */
fun BaseSimpleActivity.launchRaabtaCallIntent(recipient: String, handle: PhoneAccountHandle? = null) {
    if (recipient.isBlank()) { toast("Enter a phone number first."); return }
    handlePermission(PERMISSION_CALL_PHONE) { granted ->
        val request = if (granted) {
            Intent(this, DialerActivity::class.java).setAction(Intent.ACTION_CALL)
        } else {
            // Denied permission must open a keypad, never the call-executing activity.
            Intent(this, DialpadActivity::class.java).setAction(Intent.ACTION_DIAL)
        }
        request.data = Uri.fromParts("tel", recipient, null)
        if (granted && handle != null) request.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        try { startActivity(request) }
        catch (_: ActivityNotFoundException) { toast("Raabta's calling screen is unavailable. Please reinstall the latest APK.") }
        catch (failure: Exception) { showErrorToast(failure) }
    }
}
