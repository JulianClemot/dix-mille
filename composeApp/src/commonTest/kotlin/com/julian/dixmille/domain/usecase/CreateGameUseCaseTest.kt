package com.julian.dixmille.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateGameUseCaseTest {

    private lateinit var gameRepository: FakeGameRepository
    private lateinit var rulesRepository: FakeGameRulesRepository
    private lateinit var useCase: CreateGameUseCase

    @BeforeTest
    fun setup() {
        gameRepository = FakeGameRepository()
        rulesRepository = FakeGameRulesRepository()
        useCase = CreateGameUseCase(gameRepository, rulesRepository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun should_createGame_when_validPlayerCountProvided() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol"))

        assertTrue(result.isSuccess)
        val game = result.getOrThrow()
        assertEquals(3, game.players.size)
        assertEquals("Alice", game.players[0].name)
        assertEquals("Bob", game.players[1].name)
        assertEquals("Carol", game.players[2].name)
    }

    @Test
    fun should_initializePlayersWithDefaultState_when_gameCreated() = runTest {
        val result = useCase(listOf("Alice", "Bob"))

        val game = result.getOrThrow()
        game.players.forEach { player ->
            assertEquals(0, player.totalScore)
            assertFalse(player.hasEnteredGame)
            assertEquals(0, player.consecutiveBusts)
            assertFalse(player.hasPlayedFinalRound)
        }
    }

    @Test
    fun should_startFirstPlayerTurn_when_gameCreated() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol"))

        val game = result.getOrThrow()
        assertNotNull(game.players[0].currentTurn)
        assertNull(game.players[1].currentTurn)
        assertNull(game.players[2].currentTurn)
    }

    @Test
    fun should_applyCustomTargetScore_when_targetScoreOverridden() = runTest {
        val result = useCase(listOf("Alice", "Bob"), targetScore = 5000)

        val game = result.getOrThrow()
        assertEquals(5000, game.targetScore)
        assertEquals(5000, game.rules.targetScore)
    }

    @Test
    fun should_saveGame_when_gameCreated() = runTest {
        useCase(listOf("Alice", "Bob"))

        assertTrue(gameRepository.hasGame())
    }

    @Test
    fun should_trimPlayerNames_when_namesHaveWhitespace() = runTest {
        val result = useCase(listOf(" Alice ", " Bob"))

        val game = result.getOrThrow()
        assertEquals("Alice", game.players[0].name)
        assertEquals("Bob", game.players[1].name)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun should_createGame_when_exactlyTwoPlayersProvided() = runTest {
        val result = useCase(listOf("Alice", "Bob"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().players.size)
    }

    @Test
    fun should_createGame_when_exactlySixPlayersProvided() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol", "Dave", "Eve", "Frank"))

        assertTrue(result.isSuccess)
        assertEquals(6, result.getOrThrow().players.size)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun should_fail_when_fewerThanTwoPlayersProvided() = runTest {
        val result = useCase(listOf("Alice"))

        assertTrue(result.isFailure)
    }

    @Test
    fun should_fail_when_moreThanSixPlayersProvided() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace"))

        assertTrue(result.isFailure)
    }

    @Test
    fun should_fail_when_playerNameIsBlank() = runTest {
        val result = useCase(listOf("Alice", ""))

        assertTrue(result.isFailure)
    }

    @Test
    fun should_fail_when_targetScoreIsZero() = runTest {
        val result = useCase(listOf("Alice", "Bob"), targetScore = 0)

        assertTrue(result.isFailure)
    }
}
