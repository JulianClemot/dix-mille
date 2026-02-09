package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single score entry within a turn.
 * 
 * A turn can have multiple score entries as the player continues rolling.
 */
@Serializable
data class ScoreEntry(
    val id: String,
    val points: Int,
    val type: ScoreType = ScoreType.PRESET,
    val label: String? = null
)

/**
 * The type of score entry.
 */
@Serializable
enum class ScoreType {
    /**
     * Quick-tap preset score (e.g., 50, 100, 200).
     */
    PRESET,
    
    /**
     * Manually entered custom score.
     */
    CUSTOM
}
