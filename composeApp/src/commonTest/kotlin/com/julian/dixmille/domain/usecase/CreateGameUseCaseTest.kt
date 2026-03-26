package com.julian.dixmille.domain.usecase

import com.julian.dixmille.feature.game_setup.domain.usecase.CreateGameUseCase
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
    fun `Should create game when valid player count provided`() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol"))

        assertTrue(result.isSuccess)
        val game = result.getOrThrow()
        assertEquals(3, game.players.size)
        assertEquals("Alice", game.players[0].name)
        assertEquals("Bob", game.players[1].name)
        assertEquals("Carol", game.players[2].name)
    }

    @Test
    fun `Should initialize players with default state when game created`() = runTest {
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
    fun `Should start first player turn when game created`() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol"))

        val game = result.getOrThrow()
        assertNotNull(game.players[0].currentTurn)
        assertNull(game.players[1].currentTurn)
        assertNull(game.players[2].currentTurn)
    }

    @Test
    fun `Should apply custom target score when target score overridden`() = runTest {
        val result = useCase(listOf("Alice", "Bob"), targetScore = 5000)

        val game = result.getOrThrow()
        assertEquals(5000, game.targetScore)
        assertEquals(5000, game.rules.targetScore)
    }

    @Test
    fun `Should save game when game created`() = runTest {
        useCase(listOf("Alice", "Bob"))

        assertTrue(gameRepository.hasGame())
    }

    @Test
    fun `Should trim player names when names have whitespace`() = runTest {
        val result = useCase(listOf(" Alice ", " Bob"))

        val game = result.getOrThrow()
        assertEquals("Alice", game.players[0].name)
        assertEquals("Bob", game.players[1].name)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should create game when exactly two players provided`() = runTest {
        val result = useCase(listOf("Alice", "Bob"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().players.size)
    }

    @Test
    fun `Should create game when exactly six players provided`() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol", "Dave", "Eve", "Frank"))

        assertTrue(result.isSuccess)
        assertEquals(6, result.getOrThrow().players.size)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should fail when fewer than two players provided`() = runTest {
        val result = useCase(listOf("Alice"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should fail when more than six players provided`() = runTest {
        val result = useCase(listOf("Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should fail when player name is blank`() = runTest {
        val result = useCase(listOf("Alice", ""))

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should fail when target score is zero`() = runTest {
        val result = useCase(listOf("Alice", "Bob"), targetScore = 0)

        assertTrue(result.isFailure)
    }
}
