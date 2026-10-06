package org.rabta.phone.classic.extensions

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import androidx.annotation.WorkerThread

@WorkerThread
fun Context.getRaabtaContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean): Cursor? {
    check(Looper.myLooper() != Looper.getMainLooper()) { "Private contacts must be loaded off the UI thread" }
    return contentResolver.query(Uri.parse("content://$packageName.privatecontacts"), null, null,
        arrayOf(if (favoritesOnly) "1" else "0", if (withPhoneNumbersOnly) "1" else "0"), null)
}
