package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.util.UuidGenerator
import com.julian.dixmille.domain.validation.ValidationResult

/**
 * Undoes the last committed turn.
 * 
 * This use case:
 * - Reverts the last turn from history
 * - Restores the affected player's previous score
 * - Adjusts the current player index if needed
 * - Clears the current player's active turn
 * 
 * Only the most recent turn can be undone (single-level undo).
 * 
 * @param repository The game repository for persistence
 */
class UndoLastTurnUseCase(
    private val repository: GameRepository
) {
    /**
     * Undoes the last committed turn.
     * 
     * @return Result containing Unit on success, or error if no turns to undo
     */
    suspend operator fun invoke(): Result<Unit> = runCatching {
        var game = repository.getCurrentGame().getOrThrow()
        
        // Check if there's a turn to undo
        val lastTurn = game.getLastTurn()
            ?: throw IllegalStateException("No turns to undo")
        
        // Find the player who made the last turn
        val playerIndex = game.players.indexOfFirst { it.id == lastTurn.playerId }
        if (playerIndex == -1) {
            throw IllegalStateException("Player not found for turn record")
        }
        
        val player = game.players[playerIndex]

        // Restore the previous score from the turn record
        val previousScore = lastTurn.previousScore

        // If this was the player's entry turn (first scoring turn), mark as not entered
        val wasEntryTurn = lastTurn.outcome == TurnOutcome.SCORED && previousScore == 0 && player.hasEnteredGame

        // Re-derive consecutive bust count from remaining history
        val remainingHistory = game.turnHistory.dropLast(1)
        val playerTurns = remainingHistory.filter { it.playerId == player.id }
        var bustCount = 0
        for (turn in playerTurns.reversed()) {
            when (turn.outcome) {
                TurnOutcome.BUST -> bustCount++
                TurnOutcome.SCORED -> break
                TurnOutcome.SKIP -> { /* skip doesn't affect counter */ }
            }
        }

        // Update the player with reverted state
        val revertedPlayer = player.copy(
            totalScore = previousScore,
            hasEnteredGame = if (wasEntryTurn) false else player.hasEnteredGame,
            currentTurn = null,  // Clear any active turn
            hasPlayedFinalRound = false,  // Reset final round flag (conservative approach)
            consecutiveBusts = bustCount
        )
        
        // Update players list
        val updatedPlayers = game.players.toMutableList()
        updatedPlayers[playerIndex] = revertedPlayer
        
        // Remove the turn from history
        game = game.copy(players = updatedPlayers)
        game = game.undoLastTurn()
        
        // Determine who should be the current player
        // The player whose turn was undone should get their turn back
        game = game.copy(currentPlayerIndex = playerIndex)
        
        // Start a new turn for the current player
        val currentPlayer = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        
        // If undoing triggered the end of the game, revert to FINAL_ROUND or IN_PROGRESS
        if (game.gamePhase == GamePhase.ENDED) {
            // Check if any player has reached target score
            val hasPlayerReachedTarget = game.players.any { it.totalScore >= game.targetScore }
            game = game.copy(
                gamePhase = if (hasPlayerReachedTarget) GamePhase.FINAL_ROUND else GamePhase.IN_PROGRESS
            )
        }
        
        repository.saveGame(game).getOrThrow()
    }
}
