package org.rabta.phone.classic.helpers

import org.junit.Assert.*
import org.junit.Test

class DialpadInputPolicyTest {
    @Test fun ordinaryPhoneNumberIsNotASecretCode() {
        assertNull(DialpadInputPolicy.secretCode("+123456789"))
    }
    @Test fun networkUssdIsNotAnAndroidSecretCode() {
        assertNull(DialpadInputPolicy.secretCode("*123#"))
    }
    @Test fun incompleteAndEmptyCodesAreNotExecuted() {
        assertNull(DialpadInputPolicy.secretCode("*#*#123"))
        assertNull(DialpadInputPolicy.secretCode("*#*##*#*"))
        assertNull(DialpadInputPolicy.secretCode(""))
    }
    @Test fun completeCodeParsesWithoutExecutingAnything() {
        assertEquals("123", DialpadInputPolicy.secretCode("*#*#123#*#*"))
    }
}
