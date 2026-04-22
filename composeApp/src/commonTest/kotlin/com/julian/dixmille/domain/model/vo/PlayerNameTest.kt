package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerNameTest {

    @Test
    fun `Should create name when value is valid`() {
        val name = PlayerName("Alice")
        assertEquals("Alice", name.value)
    }

    @Test
    fun `Should throw when name is blank`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerName("   ")
        }
    }

    @Test
    fun `Should throw when name exceeds 30 characters`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerName("A".repeat(31))
        }
    }
}
