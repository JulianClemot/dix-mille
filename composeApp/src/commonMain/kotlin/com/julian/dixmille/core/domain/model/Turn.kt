package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
/**
 * Represents a single turn in the game.
 *
 * A turn contains multiple score entries as the player continues rolling.
 * If the player busts, all accumulated points are lost.
 */
data class Turn(
    val id: TurnId,
    val entries: List<ScoreEntry> = emptyList(),
    val isBusted: Boolean = false
) {
    /**
     * The total points accumulated in this turn.
     * Returns Score.ZERO if the turn is busted.
     */
    val turnTotal: Score
        get() = if (isBusted) Score.ZERO else entries.fold(Score.ZERO) { acc, entry -> acc + entry.points }

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
