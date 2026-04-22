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
 * Handles a bust (no scoring dice rolled).
 *
 * When a player busts:
 * - All accumulated turn points are lost
 * - Turn advances to next player
 * - If in final round, mark player as having played
 *
 * @param repository The game repository for persistence
 * @param validator The validator for score and game state checks
 */
class BustTurnUseCase(
    private val repository: GameRepository,
    private val validator: ScoreValidator
) {

    /**
     * Busts the current turn and advances to the next player.
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

        // Store data before busting
        val playerId = game.currentPlayer.id
        val previousScore = game.currentPlayer.totalScore

        // Increment consecutive busts
        var updatedPlayer = game.currentPlayer.copy(
            consecutiveBusts = game.currentPlayer.consecutiveBusts.increment()
        )

        // Check for bust penalty
        if (game.rules.enableBustPenalty && updatedPlayer.consecutiveBusts.value >= game.rules.consecutiveBustsForPenalty) {
            val lastScoredTurn = game.turnHistory
                .filter { it.playerId.value == playerId.value && it.outcome == TurnOutcome.SCORED }
                .lastOrNull { it.previousScore.value < updatedPlayer.totalScore.value }

            val revertToScore = lastScoredTurn?.previousScore?.value ?: 0

            updatedPlayer = updatedPlayer.copy(
                totalScore = Score(revertToScore),
                consecutiveBusts = BustCount.NONE
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
        game = game.recordTurn(playerId, Score.ZERO, TurnOutcome.BUST, previousScore)

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
            val nextPlayer = game.currentPlayer.startTurn(TurnId(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(nextPlayer)
        }

        repository.saveGame(game).getOrThrow()
    }
}
