package com.julian.dixmille.data.repository

import com.julian.dixmille.domain.model.GameRules
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRulesRepositoryImplTest {

    private lateinit var localStorage: FakeLocalStorage
    private lateinit var repository: GameRulesRepositoryImpl

    @BeforeTest
    fun setup() {
        localStorage = FakeLocalStorage()
        repository = GameRulesRepositoryImpl(localStorage)
    }

    @Test
    fun should_saveAndRetrieveRules() = runTest {
        val rules = GameRules(
            targetScore = 5_000,
            entryMinimumScore = 300,
            consecutiveBustsForPenalty = 4,
            minPlayers = 3,
            maxPlayers = 8,
            enableBustPenalty = false,
            enableFinalRound = false
        )

        repository.saveRules(rules)
        val loaded = repository.getRules().getOrThrow()

        assertEquals(rules, loaded)
    }

    @Test
    fun should_returnFailure_when_noRulesSaved() = runTest {
        val result = repository.getRules()
        assertTrue(result.isFailure)
    }

    @Test
    fun should_deleteRules() = runTest {
        repository.saveRules(GameRules.DEFAULT)
        repository.deleteRules()

        val result = repository.getRules()
        assertTrue(result.isFailure)
    }

    @Test
    fun should_preserveAllFields_when_roundTripping() = runTest {
        val rules = GameRules(
            targetScore = 7_500,
            entryMinimumScore = 0,
            consecutiveBustsForPenalty = 2,
            minPlayers = 2,
            maxPlayers = 10,
            enableBustPenalty = true,
            enableFinalRound = false
        )

        repository.saveRules(rules)
        val loaded = repository.getRules().getOrThrow()

        assertEquals(7_500, loaded.targetScore)
        assertEquals(0, loaded.entryMinimumScore)
        assertEquals(2, loaded.consecutiveBustsForPenalty)
        assertEquals(2, loaded.minPlayers)
        assertEquals(10, loaded.maxPlayers)
        assertEquals(true, loaded.enableBustPenalty)
        assertEquals(false, loaded.enableFinalRound)
    }
}
