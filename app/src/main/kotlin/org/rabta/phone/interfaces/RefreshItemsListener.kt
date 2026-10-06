package org.rabta.phone.classic.interfaces

interface RefreshItemsListener {
    fun refreshItems(invalidate: Boolean = false, callback: (() -> Unit)? = null)
}
