package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import com.julian.dixmille.feature.game_setup.domain.usecase.CreateGameUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.UpdateLastPlayedAtUseCase
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
    private lateinit var savedPlayerRepository: FakeSavedPlayerRepository
    private lateinit var useCase: CreateGameUseCase

    @BeforeTest
    fun setup() {
        gameRepository = FakeGameRepository()
        rulesRepository = FakeGameRulesRepository()
        savedPlayerRepository = FakeSavedPlayerRepository()
        useCase = CreateGameUseCase(
            repository = gameRepository,
            gameRulesRepository = rulesRepository,
            updateLastPlayedAtUseCase = UpdateLastPlayedAtUseCase(savedPlayerRepository, clock = { 42L }),
        )
    }

    private fun savedPlayers(vararg names: String): List<SavedPlayer> = names.mapIndexed { i, name ->
        SavedPlayer(
            id = PlayerId("id-$i"),
            name = PlayerName(name),
            createdAt = 0L,
            lastPlayedAt = null,
        )
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should create game when valid player count provided`() = runTest {
        savedPlayerRepository.players.addAll(savedPlayers("Alice", "Bob", "Carol"))
        val result = useCase(savedPlayers("Alice", "Bob", "Carol"))

        assertTrue(result.isSuccess)
        val game = result.getOrThrow()
        assertEquals(3, game.players.size)
        assertEquals("Alice", game.players[0].name.value)
        assertEquals("Bob", game.players[1].name.value)
        assertEquals("Carol", game.players[2].name.value)
    }

    @Test
    fun `Should initialize players with default state when game created`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob"))

        val game = result.getOrThrow()
        game.players.forEach { player ->
            assertEquals(0, player.totalScore.value)
            assertFalse(player.hasEnteredGame)
            assertEquals(0, player.consecutiveBusts.value)
            assertFalse(player.hasPlayedFinalRound)
        }
    }

    @Test
    fun `Should start first player turn when game created`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob", "Carol"))

        val game = result.getOrThrow()
        assertNotNull(game.players[0].currentTurn)
        assertNull(game.players[1].currentTurn)
        assertNull(game.players[2].currentTurn)
    }

    @Test
    fun `Should apply custom target score when target score overridden`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob"), targetScore = 5000)

        val game = result.getOrThrow()
        assertEquals(5000, game.targetScore.value)
        assertEquals(5000, game.rules.targetScore.value)
    }

    @Test
    fun `Should save game when game created`() = runTest {
        useCase(savedPlayers("Alice", "Bob"))

        assertTrue(gameRepository.hasGame())
    }

    @Test
    fun `Should trim player names when names have whitespace`() = runTest {
        val players = listOf(
            SavedPlayer(id = PlayerId("id-0"), name = PlayerName("Alice"), createdAt = 0L, lastPlayedAt = null),
            SavedPlayer(id = PlayerId("id-1"), name = PlayerName("Bob"), createdAt = 0L, lastPlayedAt = null),
        )
        val result = useCase(players)

        val game = result.getOrThrow()
        assertEquals("Alice", game.players[0].name.value)
        assertEquals("Bob", game.players[1].name.value)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should create game when exactly two players provided`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().players.size)
    }

    @Test
    fun `Should create game when exactly six players provided`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob", "Carol", "Dave", "Eve", "Frank"))

        assertTrue(result.isSuccess)
        assertEquals(6, result.getOrThrow().players.size)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should fail when fewer than two players provided`() = runTest {
        val result = useCase(savedPlayers("Alice"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should fail when more than six players provided`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should fail when target score is zero`() = runTest {
        val result = useCase(savedPlayers("Alice", "Bob"), targetScore = 0)

        assertTrue(result.isFailure)
    }

    // ── Turn order propagation ────────────────────────────────────────────────

    @Test
    fun `Should preserve non alphabetical player order when game created`() = runTest {
        val players = savedPlayers("Carol", "Alice", "Bob")
        savedPlayerRepository.players.addAll(players)

        val result = useCase(players)

        val game = result.getOrThrow()
        assertEquals("Carol", game.players[0].name.value)
        assertEquals("Alice", game.players[1].name.value)
        assertEquals("Bob", game.players[2].name.value)
    }

    @Test
    fun `Should assign first player in list as starting player when game created`() = runTest {
        val players = savedPlayers("Carol", "Alice", "Bob")
        savedPlayerRepository.players.addAll(players)

        val result = useCase(players)

        val game = result.getOrThrow()
        assertEquals(0, game.currentPlayerIndex)
        assertEquals("Carol", game.currentPlayer.name.value)
        assertNotNull(game.players[0].currentTurn)
        assertNull(game.players[1].currentTurn)
        assertNull(game.players[2].currentTurn)
    }

    // ── UpdateLastPlayedAt integration ────────────────────────────────────────

    @Test
    fun `should update lastPlayedAt for all selected players after game is created`() = runTest {
        val players = savedPlayers("Alice", "Bob")
        savedPlayerRepository.players.addAll(players)

        useCase(players)

        assertTrue(savedPlayerRepository.players.all { it.lastPlayedAt == 42L })
    }
}
