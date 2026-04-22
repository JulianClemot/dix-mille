package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TurnIdTest {

    @Test
    fun `Should create id when value is non-blank`() {
        val id = TurnId("turn-42")
        assertEquals("turn-42", id.value)
    }

    @Test
    fun `Should throw when value is blank`() {
        assertFailsWith<IllegalArgumentException> {
            TurnId("   ")
        }
    }

    @Test
    fun `Should throw when value is empty`() {
        assertFailsWith<IllegalArgumentException> {
            TurnId("")
        }
    }
}
