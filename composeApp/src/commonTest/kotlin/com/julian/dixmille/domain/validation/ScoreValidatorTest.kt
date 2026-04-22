package com.julian.dixmille.domain.validation

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.service.ValidationError
import com.julian.dixmille.core.domain.service.ValidationResult
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreValidatorTest {

    private val validator = ScoreValidator()

    // Score Entry Validation Tests

    @Test
    fun `Validate score entry should return invalid when points are zero`() {
        // Act
        val result = validator.validateScoreEntry(Score.ZERO, isPreset = false)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InvalidScoreValue)
    }

    @Test
    fun `Validate score entry should return valid when preset is valid`() {
        // Act
        val result = validator.validateScoreEntry(Score(100), isPreset = true)

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Validate score entry should return invalid when preset is invalid`() {
        // Score(700) is a valid Score (multiple of 50) but NOT in the preset list
        val result = validator.validateScoreEntry(Score(700), isPreset = true)

        // Assert
        assertTrue(result.isInvalid)
    }

    @Test
    fun `Validate score entry should return valid when custom score is positive`() {
        // Act
        val result = validator.validateScoreEntry(Score(750), isPreset = false)

        // Assert
        assertTrue(result.isValid)
    }

    // Commit Turn Validation Tests

    @Test
    fun `Validate commit turn should return invalid when no turn in progress`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"))

        // Act
        val result = validator.validateCommitTurn(player)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.NoTurnInProgress)
    }

    @Test
    fun `Validate commit turn should return invalid when turn is busted`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"))
            .startTurn(TurnId("turn1"))
        val bustedPlayer = player.copy(
            currentTurn = player.currentTurn?.bust()
        )

        // Act
        val result = validator.validateCommitTurn(bustedPlayer)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.TurnAlreadyBusted)
    }

    @Test
    fun `Validate commit turn should return invalid when points are zero`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"))
            .startTurn(TurnId("turn1"))

        // Act
        val result = validator.validateCommitTurn(player)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.MustScoreToCommit)
    }

    @Test
    fun `Validate commit turn should return invalid when not entered and score is below 500`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = false)
            .startTurn(TurnId("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId("e1"), points = Score(400)))

        // Act
        val result = validator.validateCommitTurn(player)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InsufficientPointsToEnter)
    }

    @Test
    fun `Validate commit turn should return valid when not entered and score is 500 or more`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = false)
            .startTurn(TurnId("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId("e1"), points = Score(500)))

        // Act
        val result = validator.validateCommitTurn(player)

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Validate commit turn should return valid when already entered and any points`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true)
            .startTurn(TurnId("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId("e1"), points = Score(50)))

        // Act
        val result = validator.validateCommitTurn(player)

        // Assert
        assertTrue(result.isValid)
    }

    // Game Active Validation Tests

    @Test
    fun `Validate game active should return valid when in progress`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)

        // Act
        val result = validator.validateGameActive(game)

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Validate game active should return valid when in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)

        // Act
        val result = validator.validateGameActive(game)

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Validate game active should return invalid when ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.ENDED)

        // Act
        val result = validator.validateGameActive(game)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.GameAlreadyEnded)
    }

    // Player Can Act Validation Tests

    @Test
    fun `Validate player can act should return invalid when not current player`() {
        // Arrange
        val game = createTestGame()
        val wrongPlayerId = PlayerId("p2")

        // Act
        val result = validator.validatePlayerCanAct(game, wrongPlayerId)

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.NotPlayersTurn)
    }

    @Test
    fun `Validate player can act should return invalid when game ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.ENDED)

        // Act
        val result = validator.validatePlayerCanAct(game, game.currentPlayer.id)

        // Assert
        assertTrue(result.isInvalid)
    }

    @Test
    fun `Validate player can act should return valid when current player and game active`() {
        // Arrange
        val game = createTestGame()

        // Act
        val result = validator.validatePlayerCanAct(game, game.currentPlayer.id)

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Validate player can act should return invalid when final round and already played`() {
        // Arrange
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasPlayedFinalRound = true)
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        val game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p2"),
            createdAt = 0L
        )

        // Act
        val result = validator.validatePlayerCanAct(game, PlayerId("p1"))

        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.AlreadyPlayedFinalRound)
    }

    // Final Round Trigger Tests

    @Test
    fun `Should trigger final round when in progress and score is at target`() {
        // Arrange
        val player = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(10_000))
        val game = Game(
            id = GameId("game1"),
            players = listOf(player, Player(id = PlayerId("p2"), name = PlayerName("Bob"))),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )

        // Act
        val result = validator.shouldTriggerFinalRound(game)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `Should not trigger final round when in progress and score is below target`() {
        // Arrange
        val game = createTestGame()

        // Act
        val result = validator.shouldTriggerFinalRound(game)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `Should not trigger final round when already in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)

        // Act
        val result = validator.shouldTriggerFinalRound(game)

        // Assert
        assertFalse(result)
    }

    // Should End Game Tests

    @Test
    fun `Should not end game when not in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)

        // Act
        val result = validator.shouldEndGame(game)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `Should end game when in final round and all non-triggering players finished`() {
        // Arrange
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasPlayedFinalRound = true)
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(10_000))
        val game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p2"),
            createdAt = 0L
        )

        // Act
        val result = validator.shouldEndGame(game)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `Should not end game when in final round and some players not finished`() {
        // Arrange
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasPlayedFinalRound = false)
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(10_000))
        val game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p2"),
            createdAt = 0L
        )

        // Act
        val result = validator.shouldEndGame(game)

        // Assert
        assertFalse(result)
    }

    // Winner Determination Tests

    @Test
    fun `Determine winner should return null when game has not ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)

        // Act
        val winner = validator.determineWinner(game)

        // Assert
        assertNull(winner)
    }

    @Test
    fun `Determine winner should return highest scorer when game has ended`() {
        // Arrange
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(9_500))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(10_500))
        val player3 = Player(id = PlayerId("p3"), name = PlayerName("Carol"), totalScore = Score(8_000))
        val game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2, player3),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            createdAt = 0L
        )

        // Act
        val winner = validator.determineWinner(game)

        // Assert
        assertNotNull(winner)
        assertEquals("Bob", winner.name.value)
        assertEquals(10_500, winner.totalScore.value)
    }

    // Score Exceeds Target Validation Tests

    @Test
    fun `Should return valid when score does not exceed target`() {
        // Act
        val result = validator.validateScoreDoesNotExceedTarget(
            points = Score(500),
            playerCurrentScore = Score(9000),
            targetScore = TargetScore(10_000)
        )

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Should return valid when score exactly reaches target`() {
        // Act
        val result = validator.validateScoreDoesNotExceedTarget(
            points = Score(1000),
            playerCurrentScore = Score(9000),
            targetScore = TargetScore(10_000)
        )

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Should return invalid when score exceeds target`() {
        // Act
        val result = validator.validateScoreDoesNotExceedTarget(
            points = Score(1500),
            playerCurrentScore = Score(9000),
            targetScore = TargetScore(10_000)
        )

        // Assert
        assertTrue(result.isInvalid)
        val error = (result as ValidationResult.Invalid).error
        assertTrue(error is ValidationError.ScoreExceedsTarget)
        val exceedsError = error as ValidationError.ScoreExceedsTarget
        assertEquals(1500, exceedsError.points.value)
        assertEquals(9000, exceedsError.currentScore.value)
        assertEquals(10_000, exceedsError.targetScore.value)
    }

    @Test
    fun `Should return invalid when score exceeds custom target`() {
        // Act
        val result = validator.validateScoreDoesNotExceedTarget(
            points = Score(600),
            playerCurrentScore = Score(4800),
            targetScore = TargetScore(5000)
        )

        // Assert
        assertTrue(result.isInvalid)
    }

    @Test
    fun `Should format error message when score exceeds target`() {
        // Act
        val result = validator.validateScoreDoesNotExceedTarget(
            points = Score(2000),
            playerCurrentScore = Score(9500),
            targetScore = TargetScore(10_000)
        )

        // Assert
        val error = (result as ValidationResult.Invalid).error
        assertEquals(
            "Score of 2000 would exceed the target (500 points remaining)",
            error.toString()
        )
    }

    // New VO-typed tests

    @Test
    fun `Should return InvalidScoreValue with Score when score is not a preset value`() {
        // Arrange — 700 is a valid Score (multiple of 50) but not in any preset list
        val nonPresetScore = Score(700)

        // Act
        val result = validator.validateScoreEntry(nonPresetScore, isPreset = true)

        // Assert
        assertTrue(result.isInvalid)
        val error = (result as ValidationResult.Invalid).error
        assertTrue(error is ValidationError.InvalidScoreValue)
        assertEquals(700, (error as ValidationError.InvalidScoreValue).points.value)
    }

    @Test
    fun `Should return NotPlayersTurn with PlayerId when it is not player s turn`() {
        // Arrange
        val game = createTestGame()
        val notCurrentPlayerId = PlayerId("p2")

        // Act
        val result = validator.validatePlayerCanAct(game, notCurrentPlayerId)

        // Assert
        assertTrue(result.isInvalid)
        val error = (result as ValidationResult.Invalid).error
        assertTrue(error is ValidationError.NotPlayersTurn)
        assertEquals("p2", (error as ValidationError.NotPlayersTurn).playerId.value)
    }

    // Helper Methods

    private fun createTestGame(
        gamePhase: GamePhase = GamePhase.IN_PROGRESS
    ): Game {
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(1000))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(800))

        return Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = gamePhase,
            createdAt = 0L
        )
    }
}
