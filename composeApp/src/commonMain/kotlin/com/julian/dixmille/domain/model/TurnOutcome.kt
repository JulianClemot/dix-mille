package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the outcome of a completed turn.
 */
@Serializable
enum class TurnOutcome {
    /**
     * Normal turn where the player scored points.
     * Resets the consecutive bust counter.
     */
    SCORED,
    
    /**
     * Player busted (no scoring dice rolled).
     * Counts toward the 3-consecutive-bust penalty.
     */
    BUST,
    
    /**
     * Player voluntarily skipped their turn.
     * Does NOT count toward the bust penalty.
     */
    SKIP
}
