package com.julian.dixmille.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameRulesTest {

    @Test
    fun should_createWithDefaults_when_noArgumentsProvided() {
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
    fun should_matchDefault_when_comparedToCompanionDefault() {
        assertEquals(GameRules.DEFAULT, GameRules())
    }

    @Test
    fun should_throwException_when_targetScoreNotPositive() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(targetScore = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            GameRules(targetScore = -1)
        }
    }

    @Test
    fun should_throwException_when_entryMinimumScoreNegative() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(entryMinimumScore = -1)
        }
    }

    @Test
    fun should_allowZero_when_entryMinimumScoreIsZero() {
        val rules = GameRules(entryMinimumScore = 0)
        assertEquals(0, rules.entryMinimumScore)
    }

    @Test
    fun should_throwException_when_minPlayersLessThanTwo() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(minPlayers = 1)
        }
    }

    @Test
    fun should_throwException_when_maxPlayersLessThanMinPlayers() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(minPlayers = 4, maxPlayers = 3)
        }
    }

    @Test
    fun should_throwException_when_maxPlayersExceedsTen() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(maxPlayers = 11)
        }
    }

    @Test
    fun should_throwException_when_consecutiveBustsForPenaltyLessThanTwo() {
        assertFailsWith<IllegalArgumentException> {
            GameRules(consecutiveBustsForPenalty = 1)
        }
    }

    @Test
    fun should_createValidRules_when_customValuesProvided() {
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
