package com.julian.dixmille.domain.validation

/**
 * Represents the result of a validation check.
 */
sealed class ValidationResult {
    /**
     * Validation passed successfully.
     */
    data object Valid : ValidationResult()
    
    /**
     * Validation failed with a specific error.
     */
    data class Invalid(val error: ValidationError) : ValidationResult()
    
    /**
     * Returns true if validation was successful.
     */
    val isValid: Boolean
        get() = this is Valid
    
    /**
     * Returns true if validation failed.
     */
    val isInvalid: Boolean
        get() = this is Invalid
}

/**
 * Possible validation errors in the Dix Mille game.
 */
sealed class ValidationError {
    /**
     * Player has not yet entered the game (needs 500+ points in a turn).
     */
    data object InsufficientPointsToEnter : ValidationError() {
        override fun toString(): String = "Need at least 500 points in a turn to enter the game"
    }
    
    /**
     * The score value is not valid according to game rules.
     */
    data class InvalidScoreValue(val points: Int) : ValidationError() {
        override fun toString(): String = "Score of $points is not valid"
    }
    
    /**
     * The game has already ended.
     */
    data object GameAlreadyEnded : ValidationError() {
        override fun toString(): String = "Game has already ended"
    }
    
    /**
     * It's not the specified player's turn.
     */
    data class NotPlayersTurn(val playerId: String) : ValidationError() {
        override fun toString(): String = "Not player $playerId's turn"
    }
    
    /**
     * Cannot commit a turn with zero points.
     */
    data object MustScoreToCommit : ValidationError() {
        override fun toString(): String = "Must score at least one point to end turn"
    }
    
    /**
     * The turn has already been busted.
     */
    data object TurnAlreadyBusted : ValidationError() {
        override fun toString(): String = "Turn has already been busted"
    }
    
    /**
     * No turn is currently in progress.
     */
    data object NoTurnInProgress : ValidationError() {
        override fun toString(): String = "No turn in progress"
    }
    
    /**
     * Player has already played their final round turn.
     */
    data object AlreadyPlayedFinalRound : ValidationError() {
        override fun toString(): String = "Player has already played final round"
    }
}
