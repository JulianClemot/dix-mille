package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlinx.serialization.Serializable

/**
 * Represents a player in the Dix Mille game.
 */
@Serializable
data class Player(
    val id: PlayerId,
    val name: PlayerName,
    val totalScore: Score = Score.ZERO,
    val hasEnteredGame: Boolean = false,
    val currentTurn: Turn? = null,
    val hasPlayedFinalRound: Boolean = false,
    val consecutiveBusts: BustCount = BustCount.NONE
) {
    /**
     * Starts a new turn for this player.
     */
    fun startTurn(turnId: TurnId): Player {
        return copy(currentTurn = Turn(id = turnId))
    }

    /**
     * Adds a score entry to the current turn.
     *
     * @throws IllegalStateException if no turn is in progress
     */
    fun addScoreEntry(entry: ScoreEntry): Player {
        require(currentTurn != null) { "No turn in progress" }
        return copy(currentTurn = currentTurn.addEntry(entry))
    }

    /**
     * Removes the last score entry from the current turn.
     *
     * @throws IllegalStateException if no turn is in progress
     */
    fun undoLastEntry(): Player {
        require(currentTurn != null) { "No turn in progress" }
        return copy(currentTurn = currentTurn.removeLastEntry())
    }

    /**
     * Marks the current turn as busted and clears it.
     */
    fun bustTurn(): Player {
        return copy(currentTurn = null)
    }

    /**
     * Commits the current turn, adding points to the total score.
     *
     * The player enters the game if the turn total meets the entry minimum.
     *
     * @param entryMinimumScore Minimum points in a turn to enter the game
     * @return Updated player with committed turn
     * @throws IllegalStateException if no turn is in progress or turn is busted
     */
    fun commitTurn(entryMinimumScore: EntryMinimumScore = EntryMinimumScore.DEFAULT): Player {
        require(currentTurn != null) { "No turn in progress" }
        require(!currentTurn.isBusted) { "Cannot commit a busted turn" }

        val turnPoints = currentTurn.turnTotal
        val newTotalScore = totalScore + turnPoints
        val nowEntered = hasEnteredGame || turnPoints.value >= entryMinimumScore.value

        return copy(
            totalScore = if (nowEntered) newTotalScore else totalScore,
            hasEnteredGame = nowEntered,
            currentTurn = null
        )
    }

    /**
     * Marks this player as having played their final round turn.
     */
    fun markFinalRoundPlayed(): Player {
        return copy(hasPlayedFinalRound = true)
    }
}
