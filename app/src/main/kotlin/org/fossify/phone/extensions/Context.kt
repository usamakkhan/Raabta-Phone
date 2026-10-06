package org.fossify.phone.classic.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import org.fossify.commons.extensions.launchActivityIntent
import org.fossify.commons.extensions.telecomManager
import org.fossify.commons.helpers.KEY_PHONE
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.phone.classic.helpers.Config
import org.fossify.phone.classic.models.SIMAccount

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.audioManager: AudioManager
    get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager

val Context.keyguardManager: KeyguardManager
    get() = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

@SuppressLint("MissingPermission")
fun Context.getAvailableSIMCardLabels(): List<SIMAccount> {
    val simAccounts = mutableListOf<SIMAccount>()
    try {
        val subscriptions = try {
            getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        telecomManager.callCapablePhoneAccounts.forEachIndexed { index, account ->
            val phoneAccount = telecomManager.getPhoneAccount(account) ?: return@forEachIndexed
            val label = phoneAccount.label?.toString().orEmpty()
            var address = phoneAccount.address?.toString().orEmpty()
            if (address.startsWith("tel:") && address.substringAfter("tel:").isNotEmpty()) {
                address = Uri.decode(address.substringAfter("tel:"))
            }

            // Telecom does not expose a public account-to-subscription mapping. Match by
            // the account label first; use the slot only when both lists have the same size.
            val subscription = subscriptions.firstOrNull {
                val displayName = it.displayName?.toString().orEmpty()
                val carrier = it.carrierName?.toString().orEmpty()
                label.equals(displayName, true) || label.equals(carrier, true)
            } ?: subscriptions.takeIf { it.size == telecomManager.callCapablePhoneAccounts.size }
                ?.getOrNull(index)
            val slotNumber = subscription?.simSlotIndex?.takeIf { it >= 0 }?.plus(1) ?: index + 1
            val simName = subscription?.displayName?.toString()?.takeIf { it.isNotBlank() }
            val network = subscription?.carrierName?.toString()?.takeIf { it.isNotBlank() }.orEmpty()

            simAccounts.add(
                SIMAccount(
                    id = slotNumber,
                    handle = phoneAccount.accountHandle,
                    label = label.ifBlank { simName ?: "SIM $slotNumber" },
                    phoneNumber = address.substringAfter("tel:"),
                    color = phoneAccount.highlightColor,
                    slotNumber = slotNumber,
                    network = network,
                )
            )
        }
    } catch (ignored: Exception) {
    }

    return simAccounts
}

@SuppressLint("MissingPermission")
fun Context.areMultipleSIMsAvailable(): Boolean {
    return try {
        telecomManager.callCapablePhoneAccounts.size > 1
    } catch (ignored: Exception) {
        false
    }
}

fun Context.clearMissedCalls() {
    ensureBackgroundThread {
        try {
            // notification cancellation triggers MissedCallNotifier.clearMissedCalls() which, in turn,
            // should update the database and reset the cached missed call count in MissedCallNotifier.java
            // https://android.googlesource.com/platform/packages/services/Telecomm/+/master/src/com/android/server/telecom/ui/MissedCallNotifierImpl.java#170
            telecomManager.cancelMissedCallsNotification()
        } catch (ignored: Exception) {
        }
    }
}

fun Context.canLaunchAccountsConfiguration(): Boolean {
    return Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        .resolveActivity(packageManager) != null
}

fun Context.launchAccountsConfiguration() {
    startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
}

fun Activity.startAddContactIntent(phoneNumber: String) {
    Intent().apply {
        setClass(this@startAddContactIntent, org.fossify.contacts.activities.InsertOrEditContactActivity::class.java)
        action = Intent.ACTION_INSERT_OR_EDIT
        type = "vnd.android.cursor.item/contact"
        putExtra(KEY_PHONE, phoneNumber)
        startActivity(this)
    }
}
