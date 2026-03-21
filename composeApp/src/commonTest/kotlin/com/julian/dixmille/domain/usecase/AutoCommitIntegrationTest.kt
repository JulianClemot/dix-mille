package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.AddScoreEntryUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoCommitIntegrationTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var addScoreEntryUseCase: AddScoreEntryUseCase
    private lateinit var commitTurnUseCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        addScoreEntryUseCase = AddScoreEntryUseCase(repository)
        commitTurnUseCase = CommitTurnUseCase(repository)
    }

    @Test
    fun should_commitTurnAfterAddingEntry_when_entryIsValid() = runTest {
        repository.saveGame(gameWithAlice(totalScore = 500, hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 200, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(700, game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    @Test
    fun should_failCommit_when_entryMinimumNotMet() = runTest {
        repository.saveGame(gameWithAlice(totalScore = 0, hasEnteredGame = false))

        val addResult = addScoreEntryUseCase(points = 300, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isFailure)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(0, game.players[0].totalScore)
    }

    @Test
    fun should_commitTurnAfterCustomEntry_when_entryIsValid() = runTest {
        repository.saveGame(gameWithAlice(totalScore = 500, hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 750, isPreset = false, label = "Custom")
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(1250, game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAlice(
        totalScore: Int,
        hasEnteredGame: Boolean
    ): Game {
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = hasEnteredGame,
            currentTurn = Turn(id = UuidGenerator.generate())
        )
        val bob = Player(id = "p2", name = "Bob")
        return Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
