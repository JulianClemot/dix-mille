package com.julian.dixmille.domain.model.vo

import com.julian.dixmille.core.domain.model.vo.BustCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BustCountTest {

    @Test
    fun `Should create NONE with value 0`() {
        assertEquals(0, BustCount.NONE.value)
    }

    @Test
    fun `Should increment from 0 to 1`() {
        val result = BustCount.NONE.increment()
        assertEquals(1, result.value)
    }

    @Test
    fun `Should increment from 2 to 3`() {
        val two = BustCount.of(2)
        val result = two.increment()
        assertEquals(3, result.value)
    }

    @Test
    fun `Should throw when incrementing beyond 3`() {
        val three = BustCount.of(3)
        assertFailsWith<IllegalStateException> {
            three.increment()
        }
    }

    @Test
    fun `Should return true for isMaxed when value is 3`() {
        val three = BustCount.of(3)
        assertTrue(three.isMaxed())
    }

    @Test
    fun `Should return false for isMaxed when value is 2`() {
        val two = BustCount.of(2)
        assertFalse(two.isMaxed())
    }

    @Test
    fun `Should throw when constructed with negative value`() {
        assertFailsWith<IllegalArgumentException> {
            BustCount.of(-1)
        }
    }

    @Test
    fun `Should throw when constructed with value above 3`() {
        assertFailsWith<IllegalArgumentException> {
            BustCount.of(4)
        }
    }
}
