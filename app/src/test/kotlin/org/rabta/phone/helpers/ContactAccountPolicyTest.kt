package org.rabta.phone.classic.helpers

import org.junit.Assert.*
import org.junit.Test

class ContactAccountPolicyTest {
    @Test fun localContactWithoutOptionalColumnIsEditable() {
        assertTrue(ContactAccountPolicy.canEdit(null, null, null))
        assertTrue(ContactAccountPolicy.canEdit("", null, null))
    }
    @Test fun uploadCapableAccountWorksWithoutOptionalColumn() {
        assertTrue(ContactAccountPolicy.canEdit("com.google", null, true))
    }
    @Test fun downloadOnlyAccountsRemainViewOnly() {
        assertFalse(ContactAccountPolicy.canEdit("readonly.account", null, false))
    }
    @Test fun unknownAccountDoesNotGainWriteAccess() {
        assertFalse(ContactAccountPolicy.canEdit("unknown.account", null, null))
    }
    @Test fun explicitReadOnlyFlagWinsOverAccountCapabilities() {
        assertFalse(ContactAccountPolicy.canEdit("com.google", true, true))
        assertFalse(ContactAccountPolicy.canEdit(null, true, null))
    }
    @Test fun explicitWritableFlagDoesNotRequireSyncAdapterVisibility() {
        assertTrue(ContactAccountPolicy.canEdit("local.vendor.account", false, null))
    }
}
