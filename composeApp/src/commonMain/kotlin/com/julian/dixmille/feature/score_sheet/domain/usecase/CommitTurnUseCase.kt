package com.julian.dixmille.feature.score_sheet.domain.usecase

import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.service.ValidationResult
import com.julian.dixmille.core.domain.util.UuidGenerator

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
    private val validator = ScoreValidator()

    /**
     * Commits the current turn and advances to the next player.
     *
     * @return Result containing Unit on success, or validation error
     */
    suspend operator fun invoke(): Result<Unit> = runCatching {
        var game = repository.getCurrentGame().getOrThrow()

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

        // Validate turn can be committed
        val commitValidation = validator.validateCommitTurn(game.currentPlayer, game.rules)
        if (commitValidation is ValidationResult.Invalid) {
            throw IllegalStateException(commitValidation.error.toString())
        }

        // Get data before committing
        val turnPoints = game.currentPlayer.currentTurn?.turnTotal ?: Score.ZERO
        val playerId = game.currentPlayer.id
        val previousScore = game.currentPlayer.totalScore

        // Commit the turn (adds points to total, enters game if applicable)
        var updatedPlayer = game.currentPlayer.commitTurn(game.rules.entryMinimumScore)

        // Reset consecutive busts on successful turn
        updatedPlayer = updatedPlayer.copy(consecutiveBusts = BustCount.NONE)

        game = game.updateCurrentPlayer(updatedPlayer)

        // Record turn in history
        game = game.recordTurn(playerId, turnPoints, TurnOutcome.SCORED, previousScore)

        // Resolve score collisions
        game = game.resolveScoreCollisions(playerId.value)

        // Check if final round should be triggered (or game ends immediately if final round disabled)
        if (validator.shouldTriggerFinalRound(game)) {
            val finalRoundResult = game.checkAndTriggerFinalRound()
            game = finalRoundResult.game
        }

        // If game ended immediately (final round disabled), save and return
        if (game.gamePhase == GamePhase.ENDED) {
            repository.saveGame(game).getOrThrow()
            return@runCatching
        }

        // If in final round, mark player as having played
        if (game.gamePhase == GamePhase.FINAL_ROUND) {
            val playerWithFinalRound = game.currentPlayer.markFinalRoundPlayed()
            game = game.updateCurrentPlayer(playerWithFinalRound)
        }

        // Check if game should end
        if (validator.shouldEndGame(game)) {
            val endResult = game.checkAndEndGame()
            game = endResult.game
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
            val nextPlayer = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(nextPlayer)
        }

        repository.saveGame(game).getOrThrow()
    }
}
