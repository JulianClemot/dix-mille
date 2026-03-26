package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.GameRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameRulesTest {

    @Test
    fun `Should create with defaults when no arguments provided`() {
        val rules = GameRules()

        assertEquals(10_000, rules.targetScore)
        assertEquals(500, rules.entryMinimumScore)
        assertEquals(3, rules.consecutiveBustsForPenalty)
        assertEquals(2, rules.minPlayers)
        assertEquals(6, rules.maxPlayers)
        assertTrue(rules.enableBustPenalty)
        assertTrue(rules.enableFinalRound)
    }

    @Test
    fun `Should match default when compared to companion default`() {
        assertEquals(GameRules.DEFAULT, GameRules())
    }

    @Test
    fun `Should throw exception when target score is not positive`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(targetScore = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            GameRules(targetScore = -1)
        }
    }

    @Test
    fun `Should throw exception when entry minimum score is negative`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(entryMinimumScore = -1)
        }
    }

    @Test
    fun `Should allow zero when entry minimum score is zero`() {
        val rules = GameRules(entryMinimumScore = 0)
        assertEquals(0, rules.entryMinimumScore)
    }

    @Test
    fun `Should throw exception when min players is less than two`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(minPlayers = 1)
        }
    }

    @Test
    fun `Should throw exception when max players is less than min players`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(minPlayers = 4, maxPlayers = 3)
        }
    }

    @Test
    fun `Should throw exception when max players exceeds ten`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(maxPlayers = 11)
        }
    }

    @Test
    fun `Should throw exception when consecutive busts for penalty is less than two`() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(consecutiveBustsForPenalty = 1)
        }
    }

    @Test
    fun `Should create valid rules when custom values provided`() {
        val rules = GameRules(
            targetScore = 5_000,
            entryMinimumScore = 300,
            consecutiveBustsForPenalty = 4,
            minPlayers = 3,
            maxPlayers = 8,
            enableBustPenalty = false,
            enableFinalRound = false
        )

        assertEquals(5_000, rules.targetScore)
        assertEquals(300, rules.entryMinimumScore)
        assertEquals(4, rules.consecutiveBustsForPenalty)
        assertEquals(3, rules.minPlayers)
        assertEquals(8, rules.maxPlayers)
        assertEquals(false, rules.enableBustPenalty)
        assertEquals(false, rules.enableFinalRound)
    }
}
