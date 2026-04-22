package com.julian.dixmille.data.repository

import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.feature.game_rules.data.repository.GameRulesRepositoryImpl
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
    fun `Should save and retrieve rules`() = runTest {
        val rules = GameRules(
            targetScore = TargetScore(5_000),
            entryMinimumScore = EntryMinimumScore(300),
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
    fun `Should return failure when no rules saved`() = runTest {
        val result = repository.getRules()
        assertTrue(result.isFailure)
    }

    @Test
    fun `Should delete rules`() = runTest {
        repository.saveRules(GameRules.DEFAULT)
        repository.deleteRules()

        val result = repository.getRules()
        assertTrue(result.isFailure)
    }

    @Test
    fun `Should preserve all fields when round tripping`() = runTest {
        val rules = GameRules(
            targetScore = TargetScore(7_500),
            entryMinimumScore = EntryMinimumScore.ZERO,
            consecutiveBustsForPenalty = 2,
            minPlayers = 2,
            maxPlayers = 10,
            enableBustPenalty = true,
            enableFinalRound = false
        )

        repository.saveRules(rules)
        val loaded = repository.getRules().getOrThrow()

        assertEquals(7_500, loaded.targetScore.value)
        assertEquals(0, loaded.entryMinimumScore.value)
        assertEquals(2, loaded.consecutiveBustsForPenalty)
        assertEquals(2, loaded.minPlayers)
        assertEquals(10, loaded.maxPlayers)
        assertEquals(true, loaded.enableBustPenalty)
        assertEquals(false, loaded.enableFinalRound)
    }
}
