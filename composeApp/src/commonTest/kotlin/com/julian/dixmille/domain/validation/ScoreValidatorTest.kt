package com.julian.dixmille.domain.validation

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.validation.ScoreValidator
import com.julian.dixmille.core.domain.validation.ValidationError
import com.julian.dixmille.core.domain.validation.ValidationResult
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreValidatorTest {
    
    // Score Entry Validation Tests
    
    @Test
    fun `Validate score entry should return invalid when points are negative`() {
        // Act
        val result = ScoreValidator.validateScoreEntry(-100, isPreset = false)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InvalidScoreValue)
    }
    
    @Test
    fun `Validate score entry should return invalid when points are zero`() {
        // Act
        val result = ScoreValidator.validateScoreEntry(0, isPreset = false)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun `Validate score entry should return valid when preset is valid`() {
        // Act
        val result = ScoreValidator.validateScoreEntry(100, isPreset = true)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun `Validate score entry should return invalid when preset is invalid`() {
        // Act
        val result = ScoreValidator.validateScoreEntry(75, isPreset = true)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun `Validate score entry should return valid when custom score is positive`() {
        // Act
        val result = ScoreValidator.validateScoreEntry(750, isPreset = false)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    // Commit Turn Validation Tests
    
    @Test
    fun `Validate commit turn should return invalid when no turn in progress`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.NoTurnInProgress)
    }
    
    @Test
    fun `Validate commit turn should return invalid when turn is busted`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"))
            .startTurn(TurnId.of("turn1"))
        val bustedPlayer = player.copy(
            currentTurn = player.currentTurn?.bust()
        )
        
        // Act
        val result = ScoreValidator.validateCommitTurn(bustedPlayer)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.TurnAlreadyBusted)
    }
    
    @Test
    fun `Validate commit turn should return invalid when points are zero`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"))
            .startTurn(TurnId.of("turn1"))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.MustScoreToCommit)
    }
    
    @Test
    fun `Validate commit turn should return invalid when not entered and score is below 500`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasEnteredGame = false)
            .startTurn(TurnId.of("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId.of("e1"), points = Score.of(400)))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InsufficientPointsToEnter)
    }
    
    @Test
    fun `Validate commit turn should return valid when not entered and score is 500 or more`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasEnteredGame = false)
            .startTurn(TurnId.of("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId.of("e1"), points = Score.of(500)))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun `Validate commit turn should return valid when already entered and any points`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasEnteredGame = true)
            .startTurn(TurnId.of("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId.of("e1"), points = Score.of(50)))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    // Game Active Validation Tests
    
    @Test
    fun `Validate game active should return valid when in progress`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val result = ScoreValidator.validateGameActive(game)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun `Validate game active should return valid when in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)
        
        // Act
        val result = ScoreValidator.validateGameActive(game)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun `Validate game active should return invalid when ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.ENDED)
        
        // Act
        val result = ScoreValidator.validateGameActive(game)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.GameAlreadyEnded)
    }
    
    // Player Can Act Validation Tests
    
    @Test
    fun `Validate player can act should return invalid when not current player`() {
        // Arrange
        val game = createTestGame()
        val wrongPlayerId = "p2"
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, wrongPlayerId)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.NotPlayersTurn)
    }
    
    @Test
    fun `Validate player can act should return invalid when game ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.ENDED)
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id.value)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun `Validate player can act should return valid when current player and game active`() {
        // Arrange
        val game = createTestGame()
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id.value)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun `Validate player can act should return invalid when final round and already played`() {
        // Arrange
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasPlayedFinalRound = true)
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        val game = Game(
            id = "game1",
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = "p2",
            createdAt = 0L
        )
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, "p1")
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.AlreadyPlayedFinalRound)
    }
    
    // Final Round Trigger Tests
    
    @Test
    fun `Should trigger final round when in progress and score is at target`() {
        // Arrange
        val player = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000))
        val game = Game(
            id = "game1",
            players = listOf(player, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
        
        // Act
        val result = ScoreValidator.shouldTriggerFinalRound(game)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun `Should not trigger final round when in progress and score is below target`() {
        // Arrange
        val game = createTestGame()
        
        // Act
        val result = ScoreValidator.shouldTriggerFinalRound(game)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `Should not trigger final round when already in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)
        
        // Act
        val result = ScoreValidator.shouldTriggerFinalRound(game)
        
        // Assert
        assertFalse(result)
    }
    
    // Should End Game Tests
    
    @Test
    fun `Should not end game when not in final round`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val result = ScoreValidator.shouldEndGame(game)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `Should end game when in final round and all non-triggering players finished`() {
        // Arrange
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasPlayedFinalRound = true)
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(10_000))
        val game = Game(
            id = "game1",
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = "p2",
            createdAt = 0L
        )
        
        // Act
        val result = ScoreValidator.shouldEndGame(game)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun `Should not end game when in final round and some players not finished`() {
        // Arrange
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), hasPlayedFinalRound = false)
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(10_000))
        val game = Game(
            id = "game1",
            players = listOf(player1, player2),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = "p2",
            createdAt = 0L
        )
        
        // Act
        val result = ScoreValidator.shouldEndGame(game)
        
        // Assert
        assertFalse(result)
    }
    
    // Winner Determination Tests
    
    @Test
    fun `Determine winner should return null when game has not ended`() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val winner = ScoreValidator.determineWinner(game)
        
        // Assert
        assertNull(winner)
    }
    
    @Test
    fun `Determine winner should return highest scorer when game has ended`() {
        // Arrange
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(9_500))
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(10_500))
        val player3 = Player(id = PlayerId.of("p3"), name = PlayerName.of("Carol"), totalScore = Score.of(8_000))
        val game = Game(
            id = "game1",
            players = listOf(player1, player2, player3),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            createdAt = 0L
        )
        
        // Act
        val winner = ScoreValidator.determineWinner(game)
        
        // Assert
        assertNotNull(winner)
        assertEquals("Bob", winner.name.value)
        assertEquals(10_500, winner.totalScore.value)
    }
    
    // Score Exceeds Target Validation Tests

    @Test
    fun `Should return valid when score does not exceed target`() {
        // Act
        val result = ScoreValidator.validateScoreDoesNotExceedTarget(
            points = 500,
            playerCurrentScore = 9000,
            targetScore = 10_000
        )

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Should return valid when score exactly reaches target`() {
        // Act
        val result = ScoreValidator.validateScoreDoesNotExceedTarget(
            points = 1000,
            playerCurrentScore = 9000,
            targetScore = 10_000
        )

        // Assert
        assertTrue(result.isValid)
    }

    @Test
    fun `Should return invalid when score exceeds target`() {
        // Act
        val result = ScoreValidator.validateScoreDoesNotExceedTarget(
            points = 1500,
            playerCurrentScore = 9000,
            targetScore = 10_000
        )

        // Assert
        assertTrue(result.isInvalid)
        val error = (result as ValidationResult.Invalid).error
        assertTrue(error is ValidationError.ScoreExceedsTarget)
        val exceedsError = error as ValidationError.ScoreExceedsTarget
        assertEquals(1500, exceedsError.points)
        assertEquals(9000, exceedsError.currentScore)
        assertEquals(10_000, exceedsError.targetScore)
    }

    @Test
    fun `Should return invalid when score exceeds custom target`() {
        // Act
        val result = ScoreValidator.validateScoreDoesNotExceedTarget(
            points = 600,
            playerCurrentScore = 4800,
            targetScore = 5000
        )

        // Assert
        assertTrue(result.isInvalid)
    }

    @Test
    fun `Should format error message when score exceeds target`() {
        // Act
        val result = ScoreValidator.validateScoreDoesNotExceedTarget(
            points = 2000,
            playerCurrentScore = 9500,
            targetScore = 10_000
        )

        // Assert
        val error = (result as ValidationResult.Invalid).error
        assertEquals(
            "Score of 2000 would exceed the target (500 points remaining)",
            error.toString()
        )
    }

    // Helper Methods
    
    private fun createTestGame(
        gamePhase: GamePhase = GamePhase.IN_PROGRESS
    ): Game {
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(1000))
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(800))
        
        return Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = gamePhase,
            createdAt = 0L // Test timestamp
        )
    }
}
