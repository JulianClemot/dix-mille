package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastEntryUseCase
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

class UndoLastEntryUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: UndoLastEntryUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = UndoLastEntryUseCase(repository)
    }

    @Test
    fun `Should remove last entry when turn has multiple entries`() = runTest {
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(100)),
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(200)),
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(300))
            )
        )
        repository.saveGame(gameWithCurrentPlayer(currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        val resultTurn = game.currentPlayer.currentTurn!!
        assertEquals(2, resultTurn.entries.size)
        assertEquals(100, resultTurn.entries[0].points.value)
        assertEquals(200, resultTurn.entries[1].points.value)
        assertEquals(300, resultTurn.turnTotal.value)
    }

    @Test
    fun `Should empty turn when undoing only entry`() = runTest {
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500))
            )
        )
        repository.saveGame(gameWithCurrentPlayer(currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        val resultTurn = game.currentPlayer.currentTurn!!
        assertTrue(resultTurn.entries.isEmpty())
        assertEquals(Score.ZERO, resultTurn.turnTotal)
    }

    @Test
    fun `Should not change total score when entry undone`() = runTest {
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(100)),
                ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(200))
            )
        )
        repository.saveGame(gameWithCurrentPlayer(totalScore = Score.of(800), currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(Score.of(800), game.currentPlayer.totalScore)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithCurrentPlayer(
        totalScore: Score = Score.of(0),
        currentTurn: Turn = Turn(id = TurnId.of(UuidGenerator.generate()))
    ): Game {
        val player = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = currentTurn
        )
        return Game(
            id = GameId.of("game1"),
            players = listOf(player, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            createdAt = 0L
        )
    }
}
