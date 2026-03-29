package com.julian.dixmille.feature.score_sheet.domain.usecase

import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.service.ValidationResult

/**
 * Removes the last score entry from the current player's turn.
 *
 * Useful for correcting entry mistakes.
 *
 * @param repository The game repository for persistence
 */
class UndoLastEntryUseCase(
    private val repository: GameRepository
) {
    private val validator = ScoreValidator()

    /**
     * Removes the last score entry from the current turn.
     *
     * @return Result containing Unit on success, or error if no entries to undo
     */
    suspend operator fun invoke(): Result<Unit> = runCatching {
        val game = repository.getCurrentGame().getOrThrow()

        // Validate game is active
        val gameValidation = validator.validateGameActive(game)
        if (gameValidation is ValidationResult.Invalid) {
            throw IllegalStateException(gameValidation.error.toString())
        }

        // Validate player can act
        val playerValidation = validator.validatePlayerCanAct(game, game.currentPlayer.id)
        if (playerValidation is ValidationResult.Invalid) {
            throw IllegalStateException(playerValidation.error.toString())
        }

        // Check if there are entries to undo
        val currentTurn = game.currentPlayer.currentTurn
        require(currentTurn != null && currentTurn.entries.isNotEmpty()) {
            "No score entries to undo"
        }

        // Remove last entry
        val updatedPlayer = game.currentPlayer.undoLastEntry()
        val updatedGame = game.updateCurrentPlayer(updatedPlayer)

        repository.saveGame(updatedGame).getOrThrow()
    }
}
