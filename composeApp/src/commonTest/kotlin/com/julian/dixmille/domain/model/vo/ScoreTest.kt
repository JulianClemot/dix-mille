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
        val score = Score(0)
        assertEquals(0, score.value)
    }

    @Test
    fun `Should create score when value is a valid multiple of 50`() {
        val score = Score(350)
        assertEquals(350, score.value)
    }

    @Test
    fun `Should throw when score is negative`() {
        assertFailsWith<IllegalArgumentException> {
            Score(-50)
        }
    }

    @Test
    fun `Should throw when score is not a multiple of 50`() {
        assertFailsWith<IllegalArgumentException> {
            Score(123)
        }
    }

    @Test
    fun `Should return ZERO constant with value 0`() {
        assertEquals(0, Score.ZERO.value)
    }

    @Test
    fun `Should add two scores correctly`() {
        val a = Score(300)
        val b = Score(200)
        val result = a + b
        assertEquals(500, result.value)
    }

    @Test
    fun `Should return true for meetsEntryThreshold when score is 500`() {
        val score = Score(500)
        assertTrue(score.meetsEntryThreshold())
    }

    @Test
    fun `Should return false for meetsEntryThreshold when score is 450`() {
        val score = Score(450)
        assertFalse(score.meetsEntryThreshold())
    }
}
