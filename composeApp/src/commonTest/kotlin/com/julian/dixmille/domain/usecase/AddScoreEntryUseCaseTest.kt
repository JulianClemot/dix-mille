package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreType
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.AddScoreEntryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddScoreEntryUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: AddScoreEntryUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = AddScoreEntryUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun should_addPresetEntry_when_validScoreProvided() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 100, isPreset = true)

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        val turn = game.currentPlayer.currentTurn!!
        assertEquals(1, turn.entries.size)
        assertEquals(100, turn.entries[0].points)
        assertEquals(ScoreType.PRESET, turn.entries[0].type)
    }

    @Test
    fun should_addCustomEntry_when_isPresetIsFalse() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 750, isPreset = false, label = "Custom")

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        val entry = game.currentPlayer.currentTurn!!.entries[0]
        assertEquals(ScoreType.CUSTOM, entry.type)
        assertEquals("Custom", entry.label)
        assertEquals(750, entry.points)
    }

    @Test
    fun should_accumulateTurnTotal_when_multipleEntriesAdded() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        useCase(points = 100, isPreset = true)
        useCase(points = 200, isPreset = true)

        val game = repository.getCurrentGame().getOrThrow()
        val turn = game.currentPlayer.currentTurn!!
        assertEquals(2, turn.entries.size)
        assertEquals(300, turn.turnTotal)
    }

    @Test
    fun should_saveGame_when_entryAdded() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        useCase(points = 200, isPreset = true)

        assertTrue(repository.hasGame())
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(1, saved.currentPlayer.currentTurn!!.entries.size)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun should_allowEntry_when_scoreExactlyReachesTarget() = runTest {
        repository.saveGame(gameWithCurrentPlayer(totalScore = 9900, targetScore = 10_000))

        val result = useCase(points = 100, isPreset = true)

        assertTrue(result.isSuccess)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun should_rejectEntry_when_scoreIsZero() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 0)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun should_rejectEntry_when_scoreIsNegative() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = -50)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun should_rejectEntry_when_gameHasEnded() = runTest {
        repository.saveGame(gameWithCurrentPlayer(phase = GamePhase.ENDED))

        val result = useCase(points = 100)

        assertTrue(result.isFailure)
    }

    @Test
    fun should_rejectEntry_when_scoreWouldExceedTarget() = runTest {
        repository.saveGame(gameWithCurrentPlayer(totalScore = 9800, targetScore = 10_000))

        val result = useCase(points = 300)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun should_rejectEntry_when_playerAlreadyPlayedFinalRound() = runTest {
        repository.saveGame(gameWithCurrentPlayer(
            phase = GamePhase.FINAL_ROUND,
            hasPlayedFinalRound = true
        ))

        val result = useCase(points = 100)

        assertTrue(result.isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithCurrentPlayer(
        totalScore: Int = 0,
        targetScore: Int = 10_000,
        phase: GamePhase = GamePhase.IN_PROGRESS,
        currentTurn: Turn? = Turn(id = UuidGenerator.generate()),
        hasPlayedFinalRound: Boolean = false
    ): Game {
        val player = Player(
            id = "p1",
            name = "Alice",
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = currentTurn,
            hasPlayedFinalRound = hasPlayedFinalRound
        )
        return Game(
            id = "game1",
            players = listOf(player, Player(id = "p2", name = "Bob")),
            targetScore = targetScore,
            currentPlayerIndex = 0,
            gamePhase = phase,
            createdAt = 0L
        )
    }
}
