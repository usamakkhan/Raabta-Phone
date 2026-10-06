package org.fossify.contacts.helpers

import java.text.Normalizer
import java.util.Locale

/** Classic mode matches the beginning of the displayed name, never hidden contact fields. */
object FullContactSearch {
    data class Query(val text: String, val digits: String?)
    fun prepare(value: String): Query {
        val numeric = value.any { it.isDigit() || it in "+*#" } &&
            value.all { it.isDigit() || it.isWhitespace() || it in "+*#()- ." }
        return Query(normalize(value), if (numeric) phoneKey(value) else null)
    }

    fun matchesName(name: String, query: Query, keypad: Boolean = false): Boolean =
        if (query.text.isEmpty()) true
        else if (query.digits != null) keypad && t9(name).startsWith(query.digits)
        else normalize(name).startsWith(query.text)

    private val marks = "\\p{M}+".toRegex()
    private val spaces = "\\s+".toRegex()
    private val nameCache = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 4096
    }
    private val t9Cache = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 4096
    }

    @Synchronized
    fun normalize(value: String): String = nameCache.getOrPut(value) {
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(marks, "").replace(spaces, " ").uppercase(Locale.ROOT)
    }

    fun phoneKey(value: String): String = buildString {
        value.forEach { char ->
            when {
                char.isDigit() -> append(Character.digit(char, 10))
                char == '+' || char == '*' || char == '#' -> append(char)
            }
        }
    }

    @Synchronized
    fun t9(value: String): String = t9Cache.getOrPut(value) {
        buildString {
            normalize(value).forEach { char ->
                when (char) {
                    in 'A'..'C', in 'А'..'Г' -> append('2')
                    in 'D'..'F', in 'Д'..'З' -> append('3')
                    in 'G'..'I', in 'И'..'Л' -> append('4')
                    in 'J'..'L', in 'М'..'П' -> append('5')
                    in 'M'..'O', in 'Р'..'У' -> append('6')
                    in 'P'..'S', in 'Ф'..'Ч' -> append('7')
                    in 'T'..'V', in 'Ш'..'Ы' -> append('8')
                    in 'W'..'Z', in 'Ь'..'Я' -> append('9')
                    else -> if (char.isDigit()) append(Character.digit(char, 10))
                }
            }
        }
    }

    fun matches(name: String, phones: List<String>, query: String, classic: Boolean, keypad: Boolean = false): Boolean {
        val fixed = normalize(query)
        if (fixed.isEmpty()) return true
        val numeric = query.any { it.isDigit() || it == '+' || it == '*' || it == '#' } &&
            query.all { it.isDigit() || it in "+*#()- ." }
        if (numeric) {
            val digits = phoneKey(query)
            val phoneMatch = phones.any {
                if (classic) phoneKey(it).startsWith(digits) else phoneKey(it).contains(digits)
            }
            val nameMatch = keypad && (if (classic) t9(name).startsWith(digits) else t9(name).contains(digits))
            return phoneMatch || nameMatch
        }
        return if (classic) normalize(name).startsWith(fixed) else normalize(name).contains(fixed)
    }
}
