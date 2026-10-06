package org.fossify.phone.classic.helpers

import org.junit.Assert.*
import org.junit.Test

class ContactEditPlanTest {
    private val name = ContactStore.Field(1, 10, "name", "Max", 0)
    private val number = ContactStore.Field(2, 10, "phone", "123", 2)
    private val otherAccount = ContactStore.Field(3, 20, "phone", "999", 2)
    @Test fun updatesOnlyChangedFieldsInChosenAccount() {
        val result = ContactEditPlan.create(listOf(name, number, otherAccount), 10, listOf(name, number.copy(value = "456")))
        assertTrue(result.deleted.isEmpty())
        assertEquals(listOf(number.copy(value = "456")), result.changed)
    }
    @Test fun blankFieldDeletesOnlyThatField() {
        val result = ContactEditPlan.create(listOf(name, number, otherAccount), 10, listOf(name, number.copy(value = "")))
        assertEquals(listOf(number), result.deleted)
        assertTrue(result.changed.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsChangesToAnotherAccount() {
        ContactEditPlan.create(listOf(name, number, otherAccount), 10, listOf(name, otherAccount.copy(value = "bad")))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateExistingIds() {
        ContactEditPlan.create(listOf(name), 10, listOf(name, name))
    }
    @Test fun newFieldsAreInsertedWithoutDeletingUnrelatedAccounts() {
        val added = ContactStore.Field(-1, 10, "email", "max@example.test", 3)
        val result = ContactEditPlan.create(listOf(name, otherAccount), 10, listOf(name, added))
        assertEquals(listOf(added), result.changed)
        assertTrue(result.deleted.isEmpty())
    }
}
