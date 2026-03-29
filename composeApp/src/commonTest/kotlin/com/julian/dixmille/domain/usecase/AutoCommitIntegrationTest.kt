package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.AddScoreEntryUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
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
    fun `Should commit turn after adding entry when entry is valid`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score.of(500), hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 200, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score.of(700), game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    @Test
    fun `Should fail commit when entry minimum not met`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score.of(0), hasEnteredGame = false))

        val addResult = addScoreEntryUseCase(points = 300, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isFailure)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score.ZERO, game.players[0].totalScore)
    }

    @Test
    fun `Should commit turn after custom entry when entry is valid`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score.of(500), hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 750, isPreset = false, label = "Custom")
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score.of(1250), game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAlice(
        totalScore: Score,
        hasEnteredGame: Boolean
    ): Game {
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = hasEnteredGame,
            currentTurn = Turn(id = TurnId.of(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
