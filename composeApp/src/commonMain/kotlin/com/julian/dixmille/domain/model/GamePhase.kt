package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the current phase of a Dix Mille game.
 */
@Serializable
enum class GamePhase {
    /**
     * Normal gameplay - players take turns in order.
     */
    IN_PROGRESS,
    
    /**
     * Final round triggered - one player has reached the target score.
     * Each other player gets exactly one more turn.
     */
    FINAL_ROUND,
    
    /**
     * Game has ended - winner has been determined.
     */
    ENDED
}
