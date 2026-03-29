package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.Score
import kotlinx.serialization.Serializable

/**
 * Represents a single score entry within a turn.
 *
 * A turn can have multiple score entries as the player continues rolling.
 */
@Serializable
data class ScoreEntry(
    val id: EntryId,
    val points: Score,
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
