package org.fossify.phone.classic.helpers

/** Only select accounts currently exposed by Telecom; null delegates selection to Android. */
object CallAccountPolicy {
    data class Selection<T>(val handle: T?, val askUser: Boolean = false)

    fun <T> select(available: List<T>, requested: T?, saved: T?, default: T?, forcePicker: Boolean): Selection<T> {
        if (forcePicker && available.size > 1) return Selection(null, true)
        for (candidate in listOf(requested, saved, default)) {
            if (candidate != null && candidate in available) return Selection(candidate)
        }
        return Selection(available.singleOrNull(), available.size > 1)
    }
}
