package org.fossify.phone.classic.helpers

/** Unknown account capabilities never prevent viewing, but must not authorize edits. */
object ContactAccountPolicy {
    fun canEdit(accountType: String?, providerReadOnly: Boolean?, supportsUploading: Boolean?): Boolean =
        if (providerReadOnly != null) !providerReadOnly
        else accountType.isNullOrBlank() || supportsUploading == true
}
