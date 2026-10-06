package org.fossify.phone.classic.models

import android.telecom.PhoneAccountHandle

data class SIMAccount(
    val id: Int,
    val handle: PhoneAccountHandle,
    val label: String,
    val phoneNumber: String,
    val color: Int,
    val slotNumber: Int = id,
    val network: String = "",
)
