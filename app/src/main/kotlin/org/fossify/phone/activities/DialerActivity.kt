package org.fossify.phone.classic.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import org.fossify.commons.helpers.PERMISSION_CALL_PHONE
import org.fossify.phone.classic.R
import org.fossify.phone.classic.extensions.getHandleToUse

class DialerActivity : SimpleActivity() {
    private var callNumber: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_CALL && intent.data?.scheme == "tel") {
            callNumber = intent.data

            // Obtain the Phone role before initiating an outgoing call.
            if (!isDefaultDialer()) {
                launchSetDefaultDialerIntent()
            } else {
                initOutgoingCall()
            }
        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }

    private fun initOutgoingCall() {
        handlePermission(PERMISSION_CALL_PHONE) { granted ->
            if (granted) placeOutgoingCall()
            else { toast("Phone permission is needed to place a call."); finish() }
        }
    }

    @SuppressLint("MissingPermission") // Reached only after the CALL_PHONE permission check above.
    private fun placeOutgoingCall() {
        try {
            val number = callNumber?.schemeSpecificPart.orEmpty()
            if (isNumberBlocked(number, getBlockedNumbers())) {
                toast(R.string.calling_blocked_number)
                finish()
                return
            }

            getHandleToUse(intent, number) { handle ->
                try {
                    Bundle().apply {
                        if (handle != null) putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                        telecomManager.placeCall(callNumber, this)
                    }
                } catch (failure: Exception) { showErrorToast(failure) }
                finally { finish() }
            }
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (!isDefaultDialer()) {
                try {
                    hideKeyboard()
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                        startActivity(this)
                    }
                    toast(R.string.default_phone_app_prompt, Toast.LENGTH_LONG)
                } catch (ignored: Exception) {
                }
                finish()
            } else {
                initOutgoingCall()
            }
        }
    }
}
