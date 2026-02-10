package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.util.UuidGenerator
import com.julian.dixmille.domain.validation.ScoreValidator
import com.julian.dixmille.domain.validation.ValidationResult

/**
 * Handles a bust (no scoring dice rolled).
 * 
 * When a player busts:
 * - All accumulated turn points are lost
 * - Turn advances to next player
 * - If in final round, mark player as having played
 * 
 * @param repository The game repository for persistence
 */
class BustTurnUseCase(
    private val repository: GameRepository
) {
    /**
     * Busts the current turn and advances to the next player.
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
        
        // Store data before busting
        val playerId = game.currentPlayer.id
        val previousScore = game.currentPlayer.totalScore
        
        // Increment consecutive busts
        var updatedPlayer = game.currentPlayer.copy(
            consecutiveBusts = game.currentPlayer.consecutiveBusts + 1
        )
        
        // Check for 3-bust penalty
        if (updatedPlayer.consecutiveBusts >= 3) {
            val lastScoredTurn = game.turnHistory
                .filter { it.playerId == playerId && it.outcome == TurnOutcome.SCORED }
                .lastOrNull { it.previousScore < updatedPlayer.totalScore }

            val revertToScore = lastScoredTurn?.previousScore ?: 0

            updatedPlayer = updatedPlayer.copy(
                totalScore = revertToScore,
                consecutiveBusts = 0
            )
        }
        
        // Bust the turn (clears turn, no points added)
        updatedPlayer = updatedPlayer.bustTurn()
        
        // If in final round, mark player as having played
        if (game.gamePhase == GamePhase.FINAL_ROUND) {
            updatedPlayer = updatedPlayer.markFinalRoundPlayed()
        }
        
        game = game.updateCurrentPlayer(updatedPlayer)
        
        // Record bust in turn history
        game = game.recordTurn(playerId, points = 0, TurnOutcome.BUST, previousScore)
        
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
