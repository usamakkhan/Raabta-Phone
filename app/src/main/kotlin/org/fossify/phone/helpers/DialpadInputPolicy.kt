package org.fossify.phone.classic.helpers

/** Parsing is side-effect free. Execution belongs only to the user's Call action. */
object DialpadInputPolicy {
    fun secretCode(number: String): String? =
        if (number.length > 8 && number.startsWith("*#*#") && number.endsWith("#*#*"))
            number.substring(4, number.length - 4) else null
}
