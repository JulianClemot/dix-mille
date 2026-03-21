package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.util.UuidGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SkipTurnUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = SkipTurnUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun should_leaveScoreUnchangedAndAdvance_when_playerSkips() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 0))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(600, game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    // ── Side effects ──────────────────────────────────────────────────────────

    @Test
    fun should_recordSkipOutcomeInHistory_when_playerSkips() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 0))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val record = game.turnHistory[0]
        assertEquals(TurnOutcome.SKIP, record.outcome)
        assertEquals(0, record.points)
        assertEquals(600, record.previousScore)
        assertEquals("p1", record.playerId)
    }

    // ── State preconditions ───────────────────────────────────────────────────

    @Test
    fun should_notIncrementBustCounter_when_playerSkips() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 1))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.players[0].consecutiveBusts)
    }

    @Test
    fun should_notResetBustCounter_when_playerSkips() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 2))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(2, game.players[0].consecutiveBusts)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAliceTurn(totalScore: Int, consecutiveBusts: Int): Game {
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = true,
            consecutiveBusts = consecutiveBusts,
            currentTurn = com.julian.dixmille.domain.model.Turn(id = UuidGenerator.generate())
        )
        val bob = Player(id = "p2", name = "Bob")
        return Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
    }
}
