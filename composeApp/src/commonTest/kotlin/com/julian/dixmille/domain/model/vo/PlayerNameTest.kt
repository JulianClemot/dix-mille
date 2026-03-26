package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerNameTest {

    @Test
    fun `Should create name when value is valid`() {
        val name = PlayerName.of("Alice")
        assertEquals("Alice", name.value)
    }

    @Test
    fun `Should trim whitespace on creation`() {
        val name = PlayerName.of("  Bob  ")
        assertEquals("Bob", name.value)
    }

    @Test
    fun `Should throw when name is blank after trimming`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerName.of("   ")
        }
    }

    @Test
    fun `Should throw when name exceeds 30 characters`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerName.of("A".repeat(31))
        }
    }
}
