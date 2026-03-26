package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryRuleTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = CommitTurnUseCase(repository)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should enter game when turn total is exactly 500`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 500))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(500, player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should reject commit when not entered and turn total is 499`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 499))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(0, player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should enter game when turn total exceeds 500`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 750))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(750, player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should set hasEnteredGame when minimum score met`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 500))

        useCase()

        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertTrue(player.hasEnteredGame)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should reject commit when not entered and turn is below minimum`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 400))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(0, player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── State preconditions ───────────────────────────────────────────────────

    @Test
    fun `Should allow any score when already entered game`() = runTest {
        repository.saveGame(gameForEntered(totalScore = 600, turnPoints = 50))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(650, player.totalScore)
    }

    // ── Configurable entry minimum ────────────────────────────────────────────

    @Test
    fun `Should enter game when custom minimum met exactly`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 300, entryMinimum = 300))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(300, player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should reject commit when custom minimum not met`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 250, entryMinimum = 300))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(0, player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameForUnentered(turnPoints: Int, entryMinimum: Int = 500): Game {
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = 0,
            hasEnteredGame = false,
            currentTurn = turnOf(turnPoints)
        )
        return Game(
            id = "game1",
            players = listOf(alice, Player(id = "p2", name = "Bob")),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            rules = GameRules.DEFAULT.copy(entryMinimumScore = entryMinimum)
        )
    }

    private fun gameForEntered(totalScore: Int, turnPoints: Int): Game {
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = turnOf(turnPoints)
        )
        return Game(
            id = "game1",
            players = listOf(alice, Player(id = "p2", name = "Bob")),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
    }

    private fun turnOf(points: Int): Turn = Turn(
        id = UuidGenerator.generate(),
        entries = listOf(ScoreEntry(id = UuidGenerator.generate(), points = points))
    )
}
