package com.julian.dixmille.domain.validation

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.PresetScores

/**
 * Validates game actions according to Dix Mille rules.
 */
object ScoreValidator {
    
    /**
     * Validates that a score entry is valid.
     * 
     * For MVP, we allow any positive score value for custom entries,
     * but preset scores must match the defined preset values.
     * 
     * @param points The score points to validate
     * @param isPreset Whether this is a preset score
     * @return Validation result
     */
    fun validateScoreEntry(points: Int, isPreset: Boolean): ValidationResult {
        if (points <= 0) {
            return ValidationResult.Invalid(
                ValidationError.InvalidScoreValue(points)
            )
        }

        // For preset scores, must match one of the defined presets
        if (isPreset && points !in PresetScores.validPresetValues) {
            return ValidationResult.Invalid(
                ValidationError.InvalidScoreValue(points)
            )
        }

        return ValidationResult.Valid
    }

    /**
     * Validates that the score would not cause the player to exceed the target score.
     *
     * @param points The score points to add
     * @param playerCurrentScore The player's current total score
     * @param targetScore The game's target score
     * @return Validation result
     */
    fun validateScoreDoesNotExceedTarget(
        points: Int,
        playerCurrentScore: Int,
        targetScore: Int
    ): ValidationResult {
        if (playerCurrentScore + points > targetScore) {
            return ValidationResult.Invalid(
                ValidationError.ScoreExceedsTarget(points, playerCurrentScore, targetScore)
            )
        }
        return ValidationResult.Valid
    }
    
    /**
     * Validates that a turn can be committed for the given player.
     *
     * Entry Rule: Player must score at least the entry minimum in a turn to "enter the game".
     * Until entered, turn points are lost (treated like a bust).
     *
     * @param player The player attempting to commit their turn
     * @param rules The game rules (used for entry minimum)
     * @return Validation result
     */
    fun validateCommitTurn(player: Player, rules: GameRules = GameRules.DEFAULT): ValidationResult {
        val currentTurn = player.currentTurn
            ?: return ValidationResult.Invalid(ValidationError.NoTurnInProgress)

        if (currentTurn.isBusted) {
            return ValidationResult.Invalid(ValidationError.TurnAlreadyBusted)
        }

        val turnTotal = currentTurn.turnTotal

        // Cannot commit zero points
        if (turnTotal == 0) {
            return ValidationResult.Invalid(ValidationError.MustScoreToCommit)
        }

        // Entry rule: if player hasn't entered, must score at least the entry minimum
        if (!player.hasEnteredGame && turnTotal < rules.entryMinimumScore) {
            return ValidationResult.Invalid(
                ValidationError.InsufficientPointsToEnter(rules.entryMinimumScore)
            )
        }

        return ValidationResult.Valid
    }
    
    /**
     * Validates that the game can accept actions (not ended).
     * 
     * @param game The current game state
     * @return Validation result
     */
    fun validateGameActive(game: Game): ValidationResult {
        if (game.gamePhase == GamePhase.ENDED) {
            return ValidationResult.Invalid(ValidationError.GameAlreadyEnded)
        }
        return ValidationResult.Valid
    }
    
    /**
     * Validates that a player can take their turn.
     * 
     * In final round, the triggering player cannot take another turn,
     * and other players can only play once.
     * 
     * @param game The current game state
     * @param playerId The player attempting to act
     * @return Validation result
     */
    fun validatePlayerCanAct(game: Game, playerId: String): ValidationResult {
        // Game must be active
        val activeValidation = validateGameActive(game)
        if (activeValidation.isInvalid) {
            return activeValidation
        }
        
        // Must be the current player's turn
        if (game.currentPlayer.id != playerId) {
            return ValidationResult.Invalid(ValidationError.NotPlayersTurn(playerId))
        }
        
        // In final round, check if player has already played
        if (game.gamePhase == GamePhase.FINAL_ROUND) {
            val player = game.players.find { it.id == playerId }
            if (player?.hasPlayedFinalRound == true) {
                return ValidationResult.Invalid(ValidationError.AlreadyPlayedFinalRound)
            }
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Checks if the final round should be triggered.
     * 
     * Final round is triggered when any player reaches or exceeds the target score.
     * 
     * @param game The current game state
     * @return True if final round should be triggered
     */
    fun shouldTriggerFinalRound(game: Game): Boolean {
        if (game.gamePhase != GamePhase.IN_PROGRESS) {
            return false
        }
        
        return game.currentPlayer.totalScore >= game.targetScore
    }
    
    /**
     * Checks if the game should end.
     * 
     * Game ends when all non-triggering players have played their final round turn.
     * 
     * @param game The current game state
     * @return True if the game should end
     */
    fun shouldEndGame(game: Game): Boolean {
        if (game.gamePhase != GamePhase.FINAL_ROUND) {
            return false
        }
        
        val triggeringPlayerId = game.triggeringPlayerId ?: return false
        
        return game.players
            .filter { it.id != triggeringPlayerId }
            .all { it.hasPlayedFinalRound }
    }
    
    /**
     * Determines the winner of the game.
     * 
     * Winner is the player with the highest total score.
     * 
     * @param game The game state (must be in ENDED phase)
     * @return The winning player, or null if game not ended or no players
     */
    fun determineWinner(game: Game): Player? {
        if (game.gamePhase != GamePhase.ENDED) {
            return null
        }
        
        return game.players.maxByOrNull { it.totalScore }
    }
}
