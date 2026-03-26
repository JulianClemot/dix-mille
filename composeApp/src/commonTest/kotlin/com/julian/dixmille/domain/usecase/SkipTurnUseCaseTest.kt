package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
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
    fun `Should leave score unchanged and advance when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 0))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(600, game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
        assertNotNull(game.players[1].currentTurn)
    }

    // ── Side effects ──────────────────────────────────────────────────────────

    @Test
    fun `Should record skip outcome in history when player skips`() = runTest {
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
    fun `Should not increment bust counter when player skips`() = runTest {
        repository.saveGame(gameWithAliceTurn(totalScore = 600, consecutiveBusts = 1))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1, game.players[0].consecutiveBusts)
    }

    @Test
    fun `Should not reset bust counter when player skips`() = runTest {
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
            currentTurn = com.julian.dixmille.core.domain.model.Turn(id = UuidGenerator.generate())
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
