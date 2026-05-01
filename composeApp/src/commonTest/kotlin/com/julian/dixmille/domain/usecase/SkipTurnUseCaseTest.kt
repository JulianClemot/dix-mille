package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
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

class SkipTurnUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = SkipTurnUseCase(repository, ScoreValidator())
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should leave score unchanged and advance when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(600), consecutiveBusts = BustCount.NONE))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(600, game.players[0].totalScore.value)
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    // ── Side effects ──────────────────────────────────────────────────────────

    @Test
    fun `Should record skip outcome in history when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(600), consecutiveBusts = BustCount.NONE))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val expectedRecord = TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("p1"),
            points = Score.ZERO,
            outcome = TurnOutcome.SKIP,
            previousScore = Score(600)
        )
        assertEquals(expectedRecord, game.turnHistory[0])
    }

    // ── State preconditions ───────────────────────────────────────────────────

    @Test
    fun `Should not increment bust counter when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(600), consecutiveBusts = BustCount(1)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.players[0].consecutiveBusts.value)
    }

    @Test
    fun `Should not reset bust counter when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score(600), consecutiveBusts = BustCount(2)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(2, game.players[0].consecutiveBusts.value)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAliceTurn(totalScore: Score, consecutiveBusts: BustCount): Game {
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = totalScore,
            hasEnteredGame = true,
            consecutiveBusts = consecutiveBusts,
            currentTurn = com.julian.dixmille.core.domain.model.Turn(id = TurnId(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
    }
}
