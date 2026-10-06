package org.rabta.phone.classic.helpers

import org.junit.Assert.*
import org.junit.Test

class ClassicSearchTest {
    @Test fun preparedSearchMatchesPrefixesAndDoesNotTreatWhitespaceAsANumber() {
        val query = ClassicSearch.prepare("m")
        assertTrue(ClassicSearch.matchesName("Max", query))
        assertFalse(ClassicSearch.matchesName("Emily", query))
        assertFalse(ClassicSearch.matchesName("Sam Max", query))
        assertTrue(ClassicSearch.matchesName("Max", ClassicSearch.prepare("629"), true))
        assertFalse(ClassicSearch.matchesName("Sam Max", ClassicSearch.prepare("629"), true))
        assertEquals("+1415", ClassicSearch.prepare("+1\t415").digits)
        assertTrue(ClassicSearch.matchesName("Anyone", ClassicSearch.prepare("   ")))
    }
    private fun match(name: String, query: String, classic: Boolean = true, keypad: Boolean = false,
                      phones: List<String> = emptyList()) = ClassicSearch.matches(name, phones, query, classic, keypad)

    @Test fun strictPrefixesExcludeMiddleNamesAndInfixes() {
        assertTrue(match("Max Carter", "M"))
        assertTrue(match("Mia", "m"))
        assertFalse(match("Emily", "M"))
        assertFalse(match("Sam", "M"))
        assertFalse(match("Anne Marie", "M"))
        assertTrue(match("Max", " MAX "))
    }

    @Test fun namesHandleAccentsWhitespaceAndNonLatinScripts() {
        assertTrue(match("Émile", "emi"))
        assertTrue(match("محمد", "مح"))
        assertTrue(match("Mary   Ann", "mary ann"))
        assertFalse(match("علی محمد", "مح"))
        assertTrue(match("Max", ""))
    }

    @Test fun keypadUsesPrefixesInsteadOfSubstrings() {
        assertTrue(match("Max", "629", keypad = true))
        assertFalse(match("Sam Max", "629", keypad = true))
        assertFalse(match("Emily", "629", keypad = true))
        assertFalse(match("Max", "629"))
        assertEquals("22233344455566677778889999", ClassicSearch.t9("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
    }

    @Test fun phoneSearchHandlesFormattingPlusAndUnicodeDigits() {
        val numbers = listOf("+1 (415) 555-0100", "0300 123 4567")
        assertTrue(match("Alice", "+1 415", phones = numbers))
        assertTrue(match("Alice", "0300", phones = numbers))
        assertTrue(match("Alice", "۰۳۰۰", phones = numbers))
        assertFalse(match("Alice", "555", phones = numbers))
        assertFalse(match("Alice", "+44", phones = numbers))
    }

    @Test fun modernModeAllowsContainsMatching() {
        assertTrue(match("Emily", "M", classic = false))
        assertTrue(match("Sam Max", "629", classic = false, keypad = true))
        assertTrue(match("Alice", "555", classic = false, phones = listOf("4155550100")))
    }

    @Test fun thousandContactsStayAccurateWithWarmCache() {
        val names = (0 until 1000).map { if (it % 2 == 0) "Max $it" else "Emily $it" }
        repeat(2) {
            assertEquals(500, names.count { match(it, "M") })
            assertEquals(500, names.count { match(it, "629", keypad = true) })
        }
    }
}
