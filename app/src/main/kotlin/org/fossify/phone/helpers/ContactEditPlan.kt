package org.fossify.phone.classic.helpers

/** Pure edit planning, independently testable before touching Android's contacts provider. */
object ContactEditPlan {
    data class Plan(val deleted: List<ContactStore.Field>, val changed: List<ContactStore.Field>)
    fun create(original: List<ContactStore.Field>, source: Long?, edited: List<ContactStore.Field>): Plan {
        val before = original.filter { it.raw == source }
        val existingIds = edited.filter { it.id > 0 }.map { it.id }
        require(existingIds.distinct().size == existingIds.size) { "A field was submitted twice" }
        edited.filter { it.id > 0 }.forEach { field ->
            require(before.any { it.id == field.id && it.kind == field.kind }) { "Field belongs to another contact or account" }
        }
        val nonempty = edited.filter { it.value.isNotBlank() }
        return Plan(before.filter { old -> nonempty.none { it.id == old.id } },
            nonempty.filter { changed -> changed.id <= 0 || before.first { it.id == changed.id }.value != changed.value })
    }
}
