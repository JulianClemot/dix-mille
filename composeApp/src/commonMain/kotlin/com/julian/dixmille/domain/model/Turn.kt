package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single turn in the game.
 * 
 * A turn contains multiple score entries as the player continues rolling.
 * If the player busts, all accumulated points are lost.
 */
@Serializable
data class Turn(
    val id: String,
    val entries: List<ScoreEntry> = emptyList(),
    val isBusted: Boolean = false
) {
    /**
     * The total points accumulated in this turn.
     * Returns 0 if the turn is busted.
     */
    val turnTotal: Int
        get() = if (isBusted) 0 else entries.sumOf { it.points }
    
    /**
     * Adds a new score entry to this turn.
     */
    fun addEntry(entry: ScoreEntry): Turn {
        return copy(entries = entries + entry)
    }
    
    /**
     * Removes the last score entry from this turn.
     */
    fun removeLastEntry(): Turn {
        return if (entries.isEmpty()) {
            this
        } else {
            copy(entries = entries.dropLast(1))
        }
    }
    
    /**
     * Marks this turn as busted.
     */
    fun bust(): Turn {
        return copy(isBusted = true)
    }
}
