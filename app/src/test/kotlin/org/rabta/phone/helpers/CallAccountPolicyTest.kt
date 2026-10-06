package org.rabta.phone.classic.helpers

import org.junit.Assert.*
import org.junit.Test

class CallAccountPolicyTest {
    private fun select(accounts: List<String> = listOf("sim1", "sim2"), requested: String? = null,
                       saved: String? = null, default: String? = null, force: Boolean = false) =
        CallAccountPolicy.select(accounts, requested, saved, default, force)

    @Test fun explicitAvailableAccountWins() {
        assertEquals("sim2", select(requested = "sim2", saved = "sim1", default = "sim1").handle)
    }
    @Test fun staleRequestedAccountFallsBackToValidSavedAccount() {
        assertEquals("sim1", select(requested = "removed", saved = "sim1").handle)
    }
    @Test fun absentRequestedAndStaleSavedAccountUseDefault() {
        assertEquals("sim2", select(saved = "removed", default = "sim2").handle)
    }
    @Test fun nullOrInvalidChoicesWithMultipleSimsAskUser() {
        assertTrue(select().askUser)
        assertTrue(select(requested = "gone", saved = "gone", default = "gone").askUser)
    }
    @Test fun noAccountsDelegatesToAndroidWithoutEmptyPicker() {
        val result = select(accounts = emptyList(), requested = "gone", force = true)
        assertNull(result.handle)
        assertFalse(result.askUser)
    }
    @Test fun oneAccountDoesNotShowPicker() {
        val result = select(accounts = listOf("sim1"), force = true)
        assertEquals("sim1", result.handle)
        assertFalse(result.askUser)
    }
    @Test fun forcedPickerOverridesRememberedChoice() {
        assertTrue(select(requested = "sim1", saved = "sim1", default = "sim1", force = true).askUser)
    }
}
