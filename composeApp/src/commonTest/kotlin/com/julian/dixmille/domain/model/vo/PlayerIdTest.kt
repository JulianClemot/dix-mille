package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerIdTest {

    @Test
    fun `Should create id when value is non-blank`() {
        val id = PlayerId.of("player-1")
        assertEquals("player-1", id.value)
    }

    @Test
    fun `Should throw when value is blank`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerId.of("   ")
        }
    }

    @Test
    fun `Should throw when value is empty`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerId.of("")
        }
    }
}
