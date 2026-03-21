package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastEntryUseCase
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
    fun should_removeLastEntry_when_turnHasMultipleEntries() = runTest {
        val turn = Turn(
            id = UuidGenerator.generate(),
            entries = listOf(
                ScoreEntry(id = UuidGenerator.generate(), points = 100),
                ScoreEntry(id = UuidGenerator.generate(), points = 200),
                ScoreEntry(id = UuidGenerator.generate(), points = 300)
            )
        )
        repository.saveGame(gameWithCurrentPlayer(currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        val resultTurn = game.currentPlayer.currentTurn!!
        assertEquals(2, resultTurn.entries.size)
        assertEquals(100, resultTurn.entries[0].points)
        assertEquals(200, resultTurn.entries[1].points)
        assertEquals(300, resultTurn.turnTotal)
    }

    @Test
    fun should_emptyTurn_when_undoingOnlyEntry() = runTest {
        val turn = Turn(
            id = UuidGenerator.generate(),
            entries = listOf(
                ScoreEntry(id = UuidGenerator.generate(), points = 500)
            )
        )
        repository.saveGame(gameWithCurrentPlayer(currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        val resultTurn = game.currentPlayer.currentTurn!!
        assertTrue(resultTurn.entries.isEmpty())
        assertEquals(0, resultTurn.turnTotal)
    }

    @Test
    fun should_notChangeTotalScore_when_entryUndone() = runTest {
        val turn = Turn(
            id = UuidGenerator.generate(),
            entries = listOf(
                ScoreEntry(id = UuidGenerator.generate(), points = 100),
                ScoreEntry(id = UuidGenerator.generate(), points = 200)
            )
        )
        repository.saveGame(gameWithCurrentPlayer(totalScore = 800, currentTurn = turn))

        useCase()

        val game = repository.getCurrentGame().getOrThrow()
        assertEquals(800, game.currentPlayer.totalScore)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithCurrentPlayer(
        totalScore: Int = 0,
        currentTurn: Turn = Turn(id = UuidGenerator.generate())
    ): Game {
        val player = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = currentTurn
        )
        return Game(
            id = "game1",
            players = listOf(player, Player(id = "p2", name = "Bob")),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            createdAt = 0L
        )
    }
}
