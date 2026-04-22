package com.julian.dixmille.feature.score_sheet.domain.usecase

import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.ScoreType
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.service.ValidationResult
import com.julian.dixmille.core.domain.util.UuidGenerator

/**
 * Adds a score entry to the current player's turn.
 *
 * @param repository The game repository for persistence
 * @param validator The validator for score and game state checks
 */
class AddScoreEntryUseCase(
    private val repository: GameRepository,
    private val validator: ScoreValidator
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
        val gameValidation = validator.validateGameActive(game)
        if (gameValidation is ValidationResult.Invalid) {
            throw IllegalStateException(gameValidation.error.toString())
        }

        // Validate score entry
        val scoreValidation = validator.validateScoreEntry(Score(points), isPreset)
        if (scoreValidation is ValidationResult.Invalid) {
            throw IllegalArgumentException(scoreValidation.error.toString())
        }

        // Validate player can act
        val playerValidation = validator.validatePlayerCanAct(game, game.currentPlayer.id)
        if (playerValidation is ValidationResult.Invalid) {
            throw IllegalStateException(playerValidation.error.toString())
        }

        // Validate score does not exceed target
        val targetValidation = validator.validateScoreDoesNotExceedTarget(
            points = Score(points),
            playerCurrentScore = game.currentPlayer.totalScore,
            targetScore = game.targetScore
        )
        if (targetValidation is ValidationResult.Invalid) {
            throw IllegalArgumentException(targetValidation.error.toString())
        }

        // Create score entry
        val entry = ScoreEntry(
            id = EntryId(UuidGenerator.generate()),
            points = Score(points),
            type = if (isPreset) ScoreType.PRESET else ScoreType.CUSTOM,
            label = label
        )

        // Add entry to current player's turn
        val updatedPlayer = game.currentPlayer.addScoreEntry(entry)
        val updatedGame = game.updateCurrentPlayer(updatedPlayer)

        repository.saveGame(updatedGame).getOrThrow()
    }
}
