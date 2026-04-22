package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
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
        addScoreEntryUseCase = AddScoreEntryUseCase(repository, ScoreValidator())
        commitTurnUseCase = CommitTurnUseCase(repository, ScoreValidator())
    }

    @Test
    fun `Should commit turn after adding entry when entry is valid`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score(500), hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 200, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score(700), game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    @Test
    fun `Should fail commit when entry minimum not met`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score(0), hasEnteredGame = false))

        val addResult = addScoreEntryUseCase(points = 300, isPreset = true)
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isFailure)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score.ZERO, game.players[0].totalScore)
    }

    @Test
    fun `Should commit turn after custom entry when entry is valid`() = runTest {
        repository.saveGame(gameWithAlice(totalScore = Score(500), hasEnteredGame = true))

        val addResult = addScoreEntryUseCase(points = 750, isPreset = false, label = "Custom")
        assertTrue(addResult.isSuccess)

        val commitResult = commitTurnUseCase()
        assertTrue(commitResult.isSuccess)

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score(1250), game.players[0].totalScore)
        assertEquals(1, game.currentPlayerIndex)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithAlice(
        totalScore: Score,
        hasEnteredGame: Boolean
    ): Game {
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = totalScore,
            hasEnteredGame = hasEnteredGame,
            currentTurn = Turn(id = TurnId(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
    }
}
