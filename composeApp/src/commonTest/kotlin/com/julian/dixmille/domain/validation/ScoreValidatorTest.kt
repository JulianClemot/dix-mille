package com.julian.dixmille.domain.validation

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.ScoreEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreValidatorTest {
    
    // Score Entry Validation Tests
    
    @Test
    fun validateScoreEntry_whenNegativePoints_shouldReturnInvalid() {
        // Act
        val result = ScoreValidator.validateScoreEntry(-100, isPreset = false)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InvalidScoreValue)
    }
    
    @Test
    fun validateScoreEntry_whenZeroPoints_shouldReturnInvalid() {
        // Act
        val result = ScoreValidator.validateScoreEntry(0, isPreset = false)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun validateScoreEntry_whenValidPreset_shouldReturnValid() {
        // Act
        val result = ScoreValidator.validateScoreEntry(100, isPreset = true)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun validateScoreEntry_whenInvalidPreset_shouldReturnInvalid() {
        // Act
        val result = ScoreValidator.validateScoreEntry(75, isPreset = true)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun validateScoreEntry_whenCustomPositive_shouldReturnValid() {
        // Act
        val result = ScoreValidator.validateScoreEntry(750, isPreset = false)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    // Commit Turn Validation Tests
    
    @Test
    fun validateCommitTurn_whenNoTurnInProgress_shouldReturnInvalid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.NoTurnInProgress)
    }
    
    @Test
    fun validateCommitTurn_whenTurnBusted_shouldReturnInvalid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
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
    fun validateCommitTurn_whenZeroPoints_shouldReturnInvalid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.MustScoreToCommit)
    }
    
    @Test
    fun validateCommitTurn_whenNotEntered_andBelow500_shouldReturnInvalid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice", hasEnteredGame = false)
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 400))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isInvalid)
        assertTrue((result as ValidationResult.Invalid).error is ValidationError.InsufficientPointsToEnter)
    }
    
    @Test
    fun validateCommitTurn_whenNotEntered_and500OrMore_shouldReturnValid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice", hasEnteredGame = false)
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 500))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun validateCommitTurn_whenAlreadyEntered_andAnyPoints_shouldReturnValid() {
        // Arrange
        val player = Player(id = "p1", name = "Alice", hasEnteredGame = true)
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 50))
        
        // Act
        val result = ScoreValidator.validateCommitTurn(player)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    // Game Active Validation Tests
    
    @Test
    fun validateGameActive_whenInProgress_shouldReturnValid() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val result = ScoreValidator.validateGameActive(game)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun validateGameActive_whenFinalRound_shouldReturnValid() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)
        
        // Act
        val result = ScoreValidator.validateGameActive(game)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun validateGameActive_whenEnded_shouldReturnInvalid() {
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
    fun validatePlayerCanAct_whenNotCurrentPlayer_shouldReturnInvalid() {
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
    fun validatePlayerCanAct_whenGameEnded_shouldReturnInvalid() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.ENDED)
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id)
        
        // Assert
        assertTrue(result.isInvalid)
    }
    
    @Test
    fun validatePlayerCanAct_whenCurrentPlayer_andGameActive_shouldReturnValid() {
        // Arrange
        val game = createTestGame()
        
        // Act
        val result = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id)
        
        // Assert
        assertTrue(result.isValid)
    }
    
    @Test
    fun validatePlayerCanAct_whenFinalRound_andAlreadyPlayed_shouldReturnInvalid() {
        // Arrange
        val player1 = Player(id = "p1", name = "Alice", hasPlayedFinalRound = true)
        val player2 = Player(id = "p2", name = "Bob")
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
    fun shouldTriggerFinalRound_whenInProgress_andScoreAtTarget_shouldReturnTrue() {
        // Arrange
        val player = Player(id = "p1", name = "Alice", totalScore = 10_000)
        val game = Game(
            id = "game1",
            players = listOf(player, Player(id = "p2", name = "Bob")),
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
    fun shouldTriggerFinalRound_whenInProgress_andScoreBelowTarget_shouldReturnFalse() {
        // Arrange
        val game = createTestGame()
        
        // Act
        val result = ScoreValidator.shouldTriggerFinalRound(game)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun shouldTriggerFinalRound_whenAlreadyInFinalRound_shouldReturnFalse() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.FINAL_ROUND)
        
        // Act
        val result = ScoreValidator.shouldTriggerFinalRound(game)
        
        // Assert
        assertFalse(result)
    }
    
    // Should End Game Tests
    
    @Test
    fun shouldEndGame_whenNotFinalRound_shouldReturnFalse() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val result = ScoreValidator.shouldEndGame(game)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun shouldEndGame_whenFinalRound_andAllNonTriggeringPlayersFinished_shouldReturnTrue() {
        // Arrange
        val player1 = Player(id = "p1", name = "Alice", hasPlayedFinalRound = true)
        val player2 = Player(id = "p2", name = "Bob", totalScore = 10_000)
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
    fun shouldEndGame_whenFinalRound_andSomePlayersNotFinished_shouldReturnFalse() {
        // Arrange
        val player1 = Player(id = "p1", name = "Alice", hasPlayedFinalRound = false)
        val player2 = Player(id = "p2", name = "Bob", totalScore = 10_000)
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
    fun determineWinner_whenGameNotEnded_shouldReturnNull() {
        // Arrange
        val game = createTestGame(gamePhase = GamePhase.IN_PROGRESS)
        
        // Act
        val winner = ScoreValidator.determineWinner(game)
        
        // Assert
        assertNull(winner)
    }
    
    @Test
    fun determineWinner_whenGameEnded_shouldReturnHighestScorer() {
        // Arrange
        val player1 = Player(id = "p1", name = "Alice", totalScore = 9_500)
        val player2 = Player(id = "p2", name = "Bob", totalScore = 10_500)
        val player3 = Player(id = "p3", name = "Carol", totalScore = 8_000)
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
        assertEquals("Bob", winner.name)
        assertEquals(10_500, winner.totalScore)
    }
    
    // Helper Methods
    
    private fun createTestGame(
        gamePhase: GamePhase = GamePhase.IN_PROGRESS
    ): Game {
        val player1 = Player(id = "p1", name = "Alice", totalScore = 1000)
        val player2 = Player(id = "p2", name = "Bob", totalScore = 800)
        
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
