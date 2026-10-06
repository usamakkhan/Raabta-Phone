package org.rabta.phone.classic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.rabta.phone.classic.activities.CallActivity
import org.rabta.phone.classic.helpers.ACCEPT_CALL
import org.rabta.phone.classic.helpers.CallManager
import org.rabta.phone.classic.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCEPT_CALL -> {
                context.startActivity(CallActivity.getStartIntent(context))
                CallManager.accept()
            }

            DECLINE_CALL -> CallManager.reject()
        }
    }
}
