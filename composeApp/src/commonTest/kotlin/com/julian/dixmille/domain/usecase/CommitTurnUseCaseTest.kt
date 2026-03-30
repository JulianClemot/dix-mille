package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.BustCount
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
        useCase = CommitTurnUseCase(repository, ScoreValidator())
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should add turn points to total score when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 300))

        val result = useCase()

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(800, game.players[0].totalScore.value)
    }

    @Test
    fun `Should clear current turn when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertNull(game.players[0].currentTurn)
    }

    @Test
    fun `Should reset bust counter when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200, consecutiveBusts = BustCount.of(2)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(0, game.players[0].consecutiveBusts.value)
    }

    @Test
    fun `Should record scored turn in history when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val record = game.turnHistory[0]
        assertEquals(TurnOutcome.SCORED, record.outcome)
        assertEquals(200, record.points.value)
        assertEquals(500, record.previousScore.value)
        assertEquals("p1", record.playerId.value)
    }

    @Test
    fun `Should advance to next player when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    @Test
    fun `Should save game when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200))

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(700, saved.players[0].totalScore.value)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should wrap to first player and increment round when last player commits`() = runTest {
        val game = gameWithBobTurn(totalScore = Score.of(500), turnPoints = 200)
        repository.saveGame(game)

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(0, saved.currentPlayerIndex)
        assertEquals(2, saved.roundNumber)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should reject commit when turn total is zero`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 0))

        val result = useCase()

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(500, game.players[0].totalScore.value)
    }

    @Test
    fun `Should reject commit when turn is busted`() = runTest {
        val bustedTurn = Turn(id = TurnId.of(UuidGenerator.generate()), isBusted = true)
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 0, currentTurn = bustedTurn))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(500, repository.getCurrentGame().getOrThrow().players[0].totalScore.value)
    }

    @Test
    fun `Should reject commit when game has ended`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(500), turnPoints = 200, phase = GamePhase.ENDED))

        val result = useCase()

        assertTrue(result.isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAliceTurn(
        totalScore: Score,
        turnPoints: Int,
        consecutiveBusts: BustCount = BustCount.NONE,
        phase: GamePhase = GamePhase.IN_PROGRESS,
        currentTurn: Turn? = null
    ): Game {
        val turn = currentTurn ?: if (turnPoints > 0) {
            Turn(
                id = TurnId.of(UuidGenerator.generate()),
                entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(turnPoints)))
            )
        } else {
            Turn(id = TurnId.of(UuidGenerator.generate()))
        }

        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = totalScore > Score.ZERO,
            currentTurn = turn,
            consecutiveBusts = consecutiveBusts
        )
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = phase,
            createdAt = 0L,
            roundNumber = 1
        )
    }

    private fun gameWithBobTurn(totalScore: Score, turnPoints: Int): Game {
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(500), hasEnteredGame = true)
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(turnPoints)))
        )
        val bob = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = totalScore,
            hasEnteredGame = totalScore > Score.ZERO,
            currentTurn = turn
        )
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
