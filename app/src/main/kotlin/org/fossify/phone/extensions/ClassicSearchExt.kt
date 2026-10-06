package org.fossify.phone.classic.extensions

import org.fossify.commons.models.contacts.Contact
import org.fossify.phone.classic.helpers.ClassicSearch

fun Contact.matchesClassicSearch(query: String, classic: Boolean, keypad: Boolean = false): Boolean =
    ClassicSearch.matches(getNameToDisplay(), phoneNumbers.map { it.value }, query, classic, keypad)

fun Contact.matchesClassicQuery(query: ClassicSearch.Query, keypad: Boolean = false): Boolean =
    ClassicSearch.matchesName(getNameToDisplay(), query, keypad) ||
        (query.digits != null && phoneNumbers.any { ClassicSearch.phoneKey(it.value).startsWith(query.digits) })
