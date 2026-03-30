package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
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
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(600), consecutiveBusts = BustCount.NONE))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(600, game.players[0].totalScore.value)
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    // ── Side effects ──────────────────────────────────────────────────────────

    @Test
    fun `Should record skip outcome in history when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(600), consecutiveBusts = BustCount.NONE))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.turnHistory.size)
        val record = game.turnHistory[0]
        assertEquals(TurnOutcome.SKIP, record.outcome)
        assertEquals(0, record.points.value)
        assertEquals(600, record.previousScore.value)
        assertEquals("p1", record.playerId.value)
    }

    // ── State preconditions ───────────────────────────────────────────────────

    @Test
    fun `Should not increment bust counter when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(600), consecutiveBusts = BustCount.of(1)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.players[0].consecutiveBusts.value)
    }

    @Test
    fun `Should not reset bust counter when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = Score.of(600), consecutiveBusts = BustCount.of(2)))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(2, game.players[0].consecutiveBusts.value)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAliceTurn(totalScore: Score, consecutiveBusts: BustCount): Game {
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = true,
            consecutiveBusts = consecutiveBusts,
            currentTurn = com.julian.dixmille.core.domain.model.Turn(id = TurnId.of(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
    }
}
