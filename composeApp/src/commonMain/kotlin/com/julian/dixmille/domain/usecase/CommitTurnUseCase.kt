package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.util.UuidGenerator
import com.julian.dixmille.domain.validation.ScoreValidator
import com.julian.dixmille.domain.validation.ValidationResult

/**
 * Commits the current player's turn, adding points to their total score.
 * 
 * Handles:
 * - Entry rule validation (500-point minimum for first scoring turn)
 * - Score addition to player total
 * - Final round triggering
 * - Game end detection
 * - Turn advancement
 * 
 * @param repository The game repository for persistence
 */
class CommitTurnUseCase(
    private val repository: GameRepository
) {
    /**
     * Commits the current turn and advances to the next player.
     * 
     * @return Result containing Unit on success, or validation error
     */
    suspend operator fun invoke(): Result<Unit> = runCatching {
        var game = repository.getCurrentGame().getOrThrow()
        
        // Validate game is active
        val gameValidation = ScoreValidator.validateGameActive(game)
        if (gameValidation is ValidationResult.Invalid) {
            throw IllegalStateException(gameValidation.error.toString())
        }
        
        // Validate player can act
        val playerValidation = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id)
        if (playerValidation is ValidationResult.Invalid) {
            throw IllegalStateException(playerValidation.error.toString())
        }
        
        // Validate turn can be committed
        val commitValidation = ScoreValidator.validateCommitTurn(game.currentPlayer)
        if (commitValidation is ValidationResult.Invalid) {
            throw IllegalStateException(commitValidation.error.toString())
        }
        
        // Get data before committing
        val turnPoints = game.currentPlayer.currentTurn?.turnTotal ?: 0
        val playerId = game.currentPlayer.id
        val previousScore = game.currentPlayer.totalScore
        
        // Commit the turn (adds points to total, enters game if applicable)
        var updatedPlayer = game.currentPlayer.commitTurn()
        
        // Reset consecutive busts on successful turn
        updatedPlayer = updatedPlayer.copy(consecutiveBusts = 0)
        
        game = game.updateCurrentPlayer(updatedPlayer)
        
        // Record turn in history
        game = game.recordTurn(playerId, turnPoints, TurnOutcome.SCORED, previousScore)
        
        // Check if final round should be triggered
        if (ScoreValidator.shouldTriggerFinalRound(game)) {
            game = game.checkAndTriggerFinalRound()
        }
        
        // If in final round, mark player as having played
        if (game.gamePhase == GamePhase.FINAL_ROUND) {
            val playerWithFinalRound = game.currentPlayer.markFinalRoundPlayed()
            game = game.updateCurrentPlayer(playerWithFinalRound)
        }
        
        // Check if game should end
        if (ScoreValidator.shouldEndGame(game)) {
            game = game.checkAndEndGame()
            repository.saveGame(game).getOrThrow()
            return@runCatching
        }
        
        // Advance to next player and start their turn
        game = game.advanceToNextPlayer()
        
        // Skip triggering player in final round
        if (game.gamePhase == GamePhase.FINAL_ROUND && 
            game.currentPlayer.id == game.triggeringPlayerId) {
            game = game.advanceToNextPlayer()
        }
        
        // Start next player's turn if game not ended
        if (game.gamePhase != GamePhase.ENDED) {
            val nextPlayer = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(nextPlayer)
        }
        
        repository.saveGame(game).getOrThrow()
    }
}
