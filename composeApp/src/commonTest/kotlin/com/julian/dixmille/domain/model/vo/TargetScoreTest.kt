package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TargetScoreTest {

    @Test
    fun `Should create when value is exactly 1000`() {
        val target = TargetScore.of(1000)
        assertEquals(1000, target.value)
    }

    @Test
    fun `Should create when value is 10000`() {
        val target = TargetScore.of(10_000)
        assertEquals(10_000, target.value)
    }

    @Test
    fun `Should throw when value is below 1000`() {
        assertFailsWith<IllegalArgumentException> {
            TargetScore.of(999)
        }
    }

    @Test
    fun `Should return DEFAULT with value 10000`() {
        assertEquals(10_000, TargetScore.DEFAULT.value)
    }
}
