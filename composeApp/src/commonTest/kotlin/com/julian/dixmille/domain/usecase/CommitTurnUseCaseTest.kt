package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
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
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 300))

        val result = useCase()

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(800, game.players[0].totalScore.value)
    }

    @Test
    fun `Should clear current turn when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertNull(game.players[0].currentTurn)
    }

    @Test
    fun `Should reset bust counter when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200, consecutiveBusts = BustCount(2)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(0, game.players[0].consecutiveBusts.value)
    }

    @Test
    fun `Should record scored turn in history when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val expectedRecord = TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("p1"),
            points = Score(200),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(500)
        )
        assertEquals(expectedRecord, game.turnHistory[0])
    }

    @Test
    fun `Should advance to next player when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    @Test
    fun `Should save game when turn committed`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200))

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(700, saved.players[0].totalScore.value)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should wrap to first player and increment round when last player commits`() = runTest {
        val game = gameWithBobTurn(totalScore = Score(500), turnPoints = 200)
        repository.saveGame(game)

        useCase()

        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(0, saved.currentPlayerIndex)
        assertEquals(2, saved.roundNumber)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should reject commit when turn total is zero`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 0))

        val result = useCase()

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(500, game.players[0].totalScore.value)
    }

    @Test
    fun `Should reject commit when turn is busted`() = runTest {
        val bustedTurn = Turn(id = TurnId(UuidGenerator.generate()), isBusted = true)
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 0, currentTurn = bustedTurn))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(500, repository.getCurrentGame().getOrThrow().players[0].totalScore.value)
    }

    @Test
    fun `Should reject commit when game has ended`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(500), turnPoints = 200, phase = GamePhase.ENDED))

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
                id = TurnId(UuidGenerator.generate()),
                entries = listOf(ScoreEntry(id = EntryId(UuidGenerator.generate()), points = Score(turnPoints)))
            )
        } else {
            Turn(id = TurnId(UuidGenerator.generate()))
        }

        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = totalScore,
            hasEnteredGame = totalScore > Score.ZERO,
            currentTurn = turn,
            consecutiveBusts = consecutiveBusts
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = phase,
            createdAt = 0L,
            roundNumber = 1
        )
    }

    private fun gameWithBobTurn(totalScore: Score, turnPoints: Int): Game {
        val alice = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(500), hasEnteredGame = true)
        val turn = Turn(
            id = TurnId(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId(UuidGenerator.generate()), points = Score(turnPoints)))
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = totalScore,
            hasEnteredGame = totalScore > Score.ZERO,
            currentTurn = turn
        )
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
