package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.GameId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameIdTest {

    @Test
    fun `Should create id when value is non-blank`() {
        val id = GameId.of("abc-123")
        assertEquals("abc-123", id.value)
    }

    @Test
    fun `Should throw when value is blank`() {
        assertFailsWith<IllegalArgumentException> {
            GameId.of("   ")
        }
    }

    @Test
    fun `Should throw when value is empty`() {
        assertFailsWith<IllegalArgumentException> {
            GameId.of("")
        }
    }
}
