package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a player in the Dix Mille game.
 */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val totalScore: Int = 0,
    val hasEnteredGame: Boolean = false,
    val currentTurn: Turn? = null,
    val hasPlayedFinalRound: Boolean = false,
    val consecutiveBusts: Int = 0  // Tracks consecutive busts for 3-bust penalty
) {
    /**
     * Starts a new turn for this player.
     */
    fun startTurn(turnId: String): Player {
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
    fun commitTurn(entryMinimumScore: Int = 500): Player {
        require(currentTurn != null) { "No turn in progress" }
        require(!currentTurn.isBusted) { "Cannot commit a busted turn" }

        val turnPoints = currentTurn.turnTotal
        val newTotalScore = totalScore + turnPoints
        val nowEntered = hasEnteredGame || turnPoints >= entryMinimumScore

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
