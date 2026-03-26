package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntryMinimumScoreTest {

    @Test
    fun `Should create when value is 500`() {
        val ems = EntryMinimumScore.of(500)
        assertEquals(500, ems.value)
    }

    @Test
    fun `Should throw when value is zero`() {
        assertFailsWith<IllegalArgumentException> {
            EntryMinimumScore.of(0)
        }
    }

    @Test
    fun `Should return DEFAULT with value 500`() {
        assertEquals(500, EntryMinimumScore.DEFAULT.value)
    }
}
