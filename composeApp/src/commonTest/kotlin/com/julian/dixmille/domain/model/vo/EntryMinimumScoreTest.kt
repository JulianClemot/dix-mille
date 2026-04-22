package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntryMinimumScoreTest {

    @Test
    fun `Should create when value is 500`() {
        val ems = EntryMinimumScore(500)
        assertEquals(500, ems.value)
    }

    @Test
    fun `Should create when value is zero`() {
        val ems = EntryMinimumScore(0)
        assertEquals(0, ems.value)
    }

    @Test
    fun `Should throw when value is negative`() {
        assertFailsWith<IllegalArgumentException> {
            EntryMinimumScore(-1)
        }
    }

    @Test
    fun `Should return ZERO constant with value 0`() {
        assertEquals(0, EntryMinimumScore.ZERO.value)
    }

    @Test
    fun `Should return DEFAULT with value 500`() {
        assertEquals(500, EntryMinimumScore.DEFAULT.value)
    }
}
