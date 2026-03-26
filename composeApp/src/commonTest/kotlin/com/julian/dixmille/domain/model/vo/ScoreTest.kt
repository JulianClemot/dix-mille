package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreTest {

    @Test
    fun `Should create score when value is zero`() {
        val score = Score.of(0)
        assertEquals(0, score.value)
    }

    @Test
    fun `Should create score when value is a valid multiple of 50`() {
        val score = Score.of(350)
        assertEquals(350, score.value)
    }

    @Test
    fun `Should throw when score is negative`() {
        assertFailsWith<IllegalArgumentException> {
            Score.of(-50)
        }
    }

    @Test
    fun `Should throw when score is not a multiple of 50`() {
        assertFailsWith<IllegalArgumentException> {
            Score.of(123)
        }
    }

    @Test
    fun `Should return ZERO constant with value 0`() {
        assertEquals(0, Score.ZERO.value)
    }

    @Test
    fun `Should add two scores correctly`() {
        val a = Score.of(300)
        val b = Score.of(200)
        val result = a + b
        assertEquals(500, result.value)
    }

    @Test
    fun `Should return true for meetsEntryThreshold when score is 500`() {
        val score = Score.of(500)
        assertTrue(score.meetsEntryThreshold())
    }

    @Test
    fun `Should return false for meetsEntryThreshold when score is 450`() {
        val score = Score.of(450)
        assertFalse(score.meetsEntryThreshold())
    }
}
