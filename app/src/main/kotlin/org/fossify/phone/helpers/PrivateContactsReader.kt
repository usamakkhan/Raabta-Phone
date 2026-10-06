package org.fossify.phone.classic.helpers

import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import org.fossify.commons.helpers.MyContactsContentProvider

/** Adapts only the legacy cursor decoder, not the app identity or provider access. */
object PrivateContactsReader {
    fun getContacts(context: Context, cursor: Cursor?) = cursor.use {
        // Commons' decoder checks an exact upstream package name before reading rows.
        // These rows are already from our own non-exported provider. All resources
        // and services still come from the real application context.
        val decoderContext = object : ContextWrapper(context) {
            override fun getPackageName() = "org.fossify.phone"
        }
        MyContactsContentProvider.getContacts(decoderContext, it)
    }
}
