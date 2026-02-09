package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.ScoreEntry
import com.julian.dixmille.domain.model.ScoreType
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.util.UuidGenerator
import com.julian.dixmille.domain.validation.ScoreValidator
import com.julian.dixmille.domain.validation.ValidationResult

/**
 * Adds a score entry to the current player's turn.
 * 
 * @param repository The game repository for persistence
 */
class AddScoreEntryUseCase(
    private val repository: GameRepository
) {
    /**
     * Adds points to the current player's turn.
     * 
     * @param points The score points to add
     * @param isPreset Whether this is a preset score
     * @param label Optional label for the score entry
     * @return Result containing the updated game, or validation error
     */
    suspend operator fun invoke(
        points: Int,
        isPreset: Boolean = false,
        label: String? = null
    ): Result<Unit> = runCatching {
        val game = repository.getCurrentGame().getOrThrow()
        
        // Validate game is active
        val gameValidation = ScoreValidator.validateGameActive(game)
        if (gameValidation is ValidationResult.Invalid) {
            throw IllegalStateException(gameValidation.error.toString())
        }
        
        // Validate score entry
        val scoreValidation = ScoreValidator.validateScoreEntry(points, isPreset)
        if (scoreValidation is ValidationResult.Invalid) {
            throw IllegalArgumentException(scoreValidation.error.toString())
        }
        
        // Validate player can act
        val playerValidation = ScoreValidator.validatePlayerCanAct(game, game.currentPlayer.id)
        if (playerValidation is ValidationResult.Invalid) {
            throw IllegalStateException(playerValidation.error.toString())
        }
        
        // Create score entry
        val entry = ScoreEntry(
            id = UuidGenerator.generate(),
            points = points,
            type = if (isPreset) ScoreType.PRESET else ScoreType.CUSTOM,
            label = label
        )
        
        // Add entry to current player's turn
        val updatedPlayer = game.currentPlayer.addScoreEntry(entry)
        val updatedGame = game.updateCurrentPlayer(updatedPlayer)
        
        repository.saveGame(updatedGame).getOrThrow()
    }
}
