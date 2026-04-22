package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.EntryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntryIdTest {

    @Test
    fun `Should create id when value is non-blank`() {
        val id = EntryId("entry-99")
        assertEquals("entry-99", id.value)
    }

    @Test
    fun `Should throw when value is blank`() {
        assertFailsWith<IllegalArgumentException> {
            EntryId("   ")
        }
    }

    @Test
    fun `Should throw when value is empty`() {
        assertFailsWith<IllegalArgumentException> {
            EntryId("")
        }
    }
}
