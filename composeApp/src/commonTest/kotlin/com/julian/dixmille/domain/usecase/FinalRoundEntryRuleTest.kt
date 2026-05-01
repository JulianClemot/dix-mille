package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinalRoundEntryRuleTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase
    private lateinit var bustTurnUseCase: BustTurnUseCase
    private lateinit var skipTurnUseCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        val validator = ScoreValidator()
        commitTurnUseCase = CommitTurnUseCase(repository, validator)
        bustTurnUseCase = BustTurnUseCase(repository, validator)
        skipTurnUseCase = SkipTurnUseCase(repository, validator)
    }

    // ── Commit: entry threshold rejection ────────────────────────────────────

    @Test
    fun `Should reject commit below entry threshold for un-entered player in FINAL_ROUND`() = runTest {
        // Arrange
        repository.saveGame(finalRoundGameWithBobTurn(turnPoints = 450))

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isFailure)
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(0), bob.totalScore)
        assertFalse(bob.hasEnteredGame)
    }

    @Test
    fun `Should reject commit at 450 points for un-entered player in FINAL_ROUND`() = runTest {
        // Arrange — 450 is the largest valid Score below 500 (boundary)
        repository.saveGame(finalRoundGameWithBobTurn(turnPoints = 450))

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isFailure)
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(0), bob.totalScore)
        assertFalse(bob.hasEnteredGame)
    }

    // ── Commit: entry success ─────────────────────────────────────────────────

    @Test
    fun `Should enter game at exactly 500 points in FINAL_ROUND`() = runTest {
        // Arrange — 500 is the minimum entry threshold (boundary)
        repository.saveGame(finalRoundGameWithBobTurn(turnPoints = 500))

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(500), bob.totalScore)
        assertTrue(bob.hasEnteredGame)
        assertTrue(bob.hasPlayedFinalRound)
    }

    @Test
    fun `Should enter game above entry threshold in FINAL_ROUND`() = runTest {
        // Arrange — happy path above threshold
        repository.saveGame(finalRoundGameWithBobTurn(turnPoints = 750))

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(750), bob.totalScore)
        assertTrue(bob.hasEnteredGame)
        assertTrue(bob.hasPlayedFinalRound)
    }

    // ── Bust: un-entered player ───────────────────────────────────────────────

    @Test
    fun `Should keep score at zero for un-entered player who busts in FINAL_ROUND`() = runTest {
        // Arrange
        repository.saveGame(finalRoundGameWithEmptyBobTurn())

        // Act
        bustTurnUseCase()

        // Assert
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(0), bob.totalScore)
        assertFalse(bob.hasEnteredGame)
    }

    @Test
    fun `Should mark hasPlayedFinalRound true for un-entered player who busts in FINAL_ROUND`() = runTest {
        // Arrange
        repository.saveGame(finalRoundGameWithEmptyBobTurn())

        // Act
        bustTurnUseCase()

        // Assert
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertTrue(bob.hasPlayedFinalRound)
    }

    // ── Skip: un-entered player ───────────────────────────────────────────────

    @Test
    fun `Should keep score at zero for un-entered player who skips in FINAL_ROUND`() = runTest {
        // Arrange
        repository.saveGame(finalRoundGameWithEmptyBobTurn())

        // Act
        skipTurnUseCase()

        // Assert
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertEquals(Score(0), bob.totalScore)
        assertFalse(bob.hasEnteredGame)
    }

    @Test
    fun `Should mark hasPlayedFinalRound true for un-entered player who skips in FINAL_ROUND`() = runTest {
        // Arrange
        repository.saveGame(finalRoundGameWithEmptyBobTurn())

        // Act
        skipTurnUseCase()

        // Assert
        val bob = repository.getCurrentGame().getOrThrow().players.first { it.id.value == "p2" }
        assertTrue(bob.hasPlayedFinalRound)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Standard FINAL_ROUND game:
     * - Alice: score=10000, hasEnteredGame=true, triggeringPlayer
     * - Bob (current, index=1): score=0, hasEnteredGame=false, turn with [turnPoints]
     */
    private fun finalRoundGameWithBobTurn(turnPoints: Int): Game {
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(0),
            hasEnteredGame = false,
            currentTurn = Turn(
                id = TurnId(UuidGenerator.generate()),
                entries = listOf(
                    ScoreEntry(
                        id = EntryId(UuidGenerator.generate()),
                        points = Score(turnPoints)
                    )
                )
            )
        )
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L
        )
    }

    /**
     * Standard FINAL_ROUND game:
     * - Alice: score=10000, hasEnteredGame=true, triggeringPlayer
     * - Bob (current, index=1): score=0, hasEnteredGame=false, empty turn (no entries)
     */
    private fun finalRoundGameWithEmptyBobTurn(): Game {
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(0),
            hasEnteredGame = false,
            currentTurn = Turn(id = TurnId(UuidGenerator.generate()))
        )
        return Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L
        )
    }
}
