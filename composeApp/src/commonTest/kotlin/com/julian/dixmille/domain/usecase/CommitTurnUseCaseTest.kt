package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitTurnUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = CommitTurnUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun should_addTurnPointsToTotalScore_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 300))

        val result = useCase()

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(800, game.players[0].totalScore)
    }

    @Test
    fun should_clearCurrentTurn_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertNull(game.players[0].currentTurn)
    }

    @Test
    fun should_resetBustCounter_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200, consecutiveBusts = 2))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(0, game.players[0].consecutiveBusts)
    }

    @Test
    fun should_recordScoredTurnInHistory_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val record = game.turnHistory[0]
        assertEquals(TurnOutcome.SCORED, record.outcome)
        assertEquals(200, record.points)
        assertEquals(500, record.previousScore)
        assertEquals("p1", record.playerId)
    }

    @Test
    fun should_advanceToNextPlayer_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    @Test
    fun should_saveGame_when_turnCommitted() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200))

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(700, saved.players[0].totalScore)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun should_wrapToFirstPlayerAndIncrementRound_when_lastPlayerCommits() = runTest {
        val game = gameWithBobTurn(totalScore = 500, turnPoints = 200)
        repository.saveGame(game)

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(0, saved.currentPlayerIndex)
        assertEquals(2, saved.roundNumber)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun should_rejectCommit_when_turnTotalIsZero() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 0))

        val result = useCase()

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(500, game.players[0].totalScore)
    }

    @Test
    fun should_rejectCommit_when_turnIsBusted() = runTest {
        val bustedTurn = Turn(id = UuidGenerator.generate(), isBusted = true)
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 0, currentTurn = bustedTurn))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(500, repository.getCurrentGame().getOrThrow().players[0].totalScore)
    }

    @Test
    fun should_rejectCommit_when_gameHasEnded() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 500, turnPoints = 200, phase = GamePhase.ENDED))

        val result = useCase()

        assertTrue(result.isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAliceTurn(
        totalScore: Int,
        turnPoints: Int,
        consecutiveBusts: Int = 0,
        phase: GamePhase = GamePhase.IN_PROGRESS,
        currentTurn: Turn? = null
    ): Game {
        val turn = currentTurn ?: if (turnPoints > 0) {
            Turn(
                id = UuidGenerator.generate(),
                entries = listOf(ScoreEntry(id = UuidGenerator.generate(), points = turnPoints))
            )
        } else {
            Turn(id = UuidGenerator.generate())
        }

        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = totalScore > 0,
            currentTurn = turn,
            consecutiveBusts = consecutiveBusts
        )
        val bob = Player(id = "p2", name = "Bob")
        return Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = phase,
            createdAt = 0L,
            roundNumber = 1
        )
    }

    private fun gameWithBobTurn(totalScore: Int, turnPoints: Int): Game {
        val alice = Player(id = "p1", name = "Alice", totalScore = 500, hasEnteredGame = true)
        val turn = Turn(
            id = UuidGenerator.generate(),
            entries = listOf(ScoreEntry(id = UuidGenerator.generate(), points = turnPoints))
        )
        val bob = Player(
            id = "p2",
            name = "Bob",
            totalScore = totalScore,
            hasEnteredGame = totalScore > 0,
            currentTurn = turn
        )
        return Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
