package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.util.UuidGenerator
import com.julian.dixmille.domain.validation.ScoreValidator
import com.julian.dixmille.domain.validation.ValidationResult

/**
 * Handles a voluntarily skipped turn.
 * 
 * When a player skips:
 * - Turn is recorded with 0 points and SKIP outcome
 * - Does NOT count toward the 3-bust penalty
 * - Turn advances to next player
 * - If in final round, mark player as having played
 * 
 * @param repository The game repository for persistence
 */
class SkipTurnUseCase(
    private val repository: GameRepository
) {
    /**
     * Skips the current turn and advances to the next player.
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
        
        // Store data before skipping
        val playerId = game.currentPlayer.id
        val previousScore = game.currentPlayer.totalScore
        
        // Clear current turn (no points, no bust increment)
        var updatedPlayer = game.currentPlayer.copy(currentTurn = null)
        
        // If in final round, mark player as having played
        if (game.gamePhase == GamePhase.FINAL_ROUND) {
            updatedPlayer = updatedPlayer.markFinalRoundPlayed()
        }
        
        game = game.updateCurrentPlayer(updatedPlayer)
        
        // Record skip in turn history (does NOT count as bust)
        game = game.recordTurn(playerId, points = 0, TurnOutcome.SKIP, previousScore)
        
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
