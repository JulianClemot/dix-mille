package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreType
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.AddScoreEntryUseCase
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
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
        useCase = AddScoreEntryUseCase(repository, ScoreValidator())
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should add preset entry when valid score provided`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 100, isPreset = true)

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        val turn = game.currentPlayer.currentTurn!!
        assertEquals(1, turn.entries.size)
        assertEquals(100, turn.entries[0].points.value)
        assertEquals(ScoreType.PRESET, turn.entries[0].type)
    }

    @Test
    fun `Should add custom entry when isPreset is false`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 750, isPreset = false, label = "Custom")

        assertTrue(result.isSuccess)
        val game = repository.getCurrentGame().getOrThrow()
        val entry = game.currentPlayer.currentTurn!!.entries[0]
        assertEquals(ScoreType.CUSTOM, entry.type)
        assertEquals("Custom", entry.label)
        assertEquals(750, entry.points.value)
    }

    @Test
    fun `Should accumulate turn total when multiple entries added`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        useCase(points = 100, isPreset = true)
        useCase(points = 200, isPreset = true)

        val game = repository.getCurrentGame().getOrThrow()
        val turn = game.currentPlayer.currentTurn!!
        assertEquals(2, turn.entries.size)
        assertEquals(300, turn.turnTotal.value)
    }

    @Test
    fun `Should save game when entry added`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        useCase(points = 200, isPreset = true)

        assertTrue(repository.hasGame())
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(1, saved.currentPlayer.currentTurn!!.entries.size)
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should allow entry when score exactly reaches target`() = runTest {
        repository.saveGame(gameWithCurrentPlayer(totalScore = Score.of(9900), targetScore = 10_000))

        val result = useCase(points = 100, isPreset = true)

        assertTrue(result.isSuccess)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should reject entry when score is zero`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = 0)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun `Should reject entry when score is negative`() = runTest {
        repository.saveGame(gameWithCurrentPlayer())

        val result = useCase(points = -50)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun `Should reject entry when game has ended`() = runTest {
        repository.saveGame(gameWithCurrentPlayer(phase = GamePhase.ENDED))

        val result = useCase(points = 100)

        assertTrue(result.isFailure)
    }

    @Test
    fun `Should reject entry when score would exceed target`() = runTest {
        repository.saveGame(gameWithCurrentPlayer(totalScore = Score.of(9800), targetScore = 10_000))

        val result = useCase(points = 300)

        assertTrue(result.isFailure)
        val game = repository.getCurrentGame().getOrThrow()
        assertTrue(game.currentPlayer.currentTurn!!.entries.isEmpty())
    }

    @Test
    fun `Should reject entry when player already played final round`() = runTest {
        repository.saveGame(gameWithCurrentPlayer(
            phase = GamePhase.FINAL_ROUND,
            hasPlayedFinalRound = true
        ))

        val result = useCase(points = 100)

        assertTrue(result.isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameWithCurrentPlayer(
        totalScore: Score = Score.of(0),
        targetScore: Int = 10_000,
        phase: GamePhase = GamePhase.IN_PROGRESS,
        currentTurn: Turn? = Turn(id = TurnId.of(UuidGenerator.generate())),
        hasPlayedFinalRound: Boolean = false
    ): Game {
        val player = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = currentTurn,
            hasPlayedFinalRound = hasPlayedFinalRound
        )
        return Game(
            id = GameId.of("game1"),
            players = listOf(player, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            targetScore = TargetScore.of(targetScore),
            currentPlayerIndex = 0,
            gamePhase = phase,
            createdAt = 0L
        )
    }
}
