package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a historical record of a completed turn.
 * 
 * Used to track round-by-round scoring history for display and undo functionality.
 * A round represents a complete cycle where all players have taken one turn.
 * Tracks the outcome (scored, bust, or skip) and the player's score before this turn
 * to enable the 3-consecutive-bust penalty reversion.
 */
@Serializable
data class TurnRecord(
    val roundNumber: Int,
    val playerId: String,
    val points: Int,
    val outcome: TurnOutcome,
    val previousScore: Int  // Player's total score BEFORE this turn (for reversion)
)
