package org.fossify.phone.classic.services

import android.telecom.Call
import android.telecom.CallScreeningService
import org.fossify.commons.extensions.baseConfig
import org.fossify.phone.classic.extensions.getRaabtaContactsCursor as getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.phone.classic.helpers.PrivateContactsReader

class SimpleCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        when {
            number != null && isNumberBlocked(number) -> {
                respondToCall(callDetails, isBlocked = true)
            }

            number != null && baseConfig.blockUnknownNumbers -> {
                ensureBackgroundThread {
                    val isUnknown = try {
                        getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use { cursor ->
                            val knownPrivate = PrivateContactsReader.getContacts(this, cursor).any { it.doesHavePhoneNumber(number) }
                            !knownPrivate && SimpleContactsHelper(this).existsSync(number, null) == ContactLookupResult.NotFound
                        }
                    } catch (_: Exception) {
                        false // If lookup fails, do not reject a potentially wanted call.
                    }
                    respondToCall(callDetails, isBlocked = isUnknown)
                }
            }

            number == null && baseConfig.blockHiddenNumbers -> {
                respondToCall(callDetails, isBlocked = true)
            }

            else -> {
                respondToCall(callDetails, isBlocked = false)
            }
        }
    }

    private fun respondToCall(callDetails: Call.Details, isBlocked: Boolean) {
        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(isBlocked)
            .setSkipNotification(isBlocked)
            .build()

        respondToCall(callDetails, response)
    }
}
