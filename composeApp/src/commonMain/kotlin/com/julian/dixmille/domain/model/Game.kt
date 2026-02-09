package com.julian.dixmille.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a complete Dix Mille game.
 */
@Serializable
data class Game(
    val id: String,
    val players: List<Player>,
    val targetScore: Int = 10_000,
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.IN_PROGRESS,
    val triggeringPlayerId: String? = null,
    val createdAt: Long,
    val turnHistory: List<TurnRecord> = emptyList(),
    val roundNumber: Int = 1
) {
    init {
        require(players.size in 2..6) { "Game must have 2-6 players" }
        require(targetScore > 0) { "Target score must be positive" }
        require(currentPlayerIndex in players.indices) { "Invalid player index" }
    }
    
    /**
     * The current player whose turn it is.
     */
    val currentPlayer: Player
        get() = players[currentPlayerIndex]
    
    /**
     * Updates the current player in the game.
     */
    fun updateCurrentPlayer(updatedPlayer: Player): Game {
        val updatedPlayers = players.toMutableList()
        updatedPlayers[currentPlayerIndex] = updatedPlayer
        return copy(players = updatedPlayers)
    }
    
    /**
     * Advances to the next player's turn.
     * 
     * Increments the round number when wrapping back to the first player,
     * indicating that all players have completed their turn in the current round.
     */
    fun advanceToNextPlayer(): Game {
        val nextIndex = (currentPlayerIndex + 1) % players.size
        // Increment round only when wrapping back to first player
        val nextRound = if (nextIndex == 0) roundNumber + 1 else roundNumber
        return copy(
            currentPlayerIndex = nextIndex,
            roundNumber = nextRound
        )
    }
    
    /**
     * Checks if the current player has reached the target score and triggers final round if needed.
     * 
     * @return Updated game in FINAL_ROUND phase if triggered, otherwise unchanged
     */
    fun checkAndTriggerFinalRound(): Game {
        if (gamePhase != GamePhase.IN_PROGRESS) {
            return this
        }
        
        if (currentPlayer.totalScore >= targetScore) {
            return copy(
                gamePhase = GamePhase.FINAL_ROUND,
                triggeringPlayerId = currentPlayer.id
            )
        }
        
        return this
    }
    
    /**
     * Checks if the game should end (all non-triggering players have played final round).
     * 
     * @return Updated game in ENDED phase if complete, otherwise unchanged
     */
    fun checkAndEndGame(): Game {
        if (gamePhase != GamePhase.FINAL_ROUND) {
            return this
        }
        
        val allNonTriggeringPlayersFinished = players
            .filter { it.id != triggeringPlayerId }
            .all { it.hasPlayedFinalRound }
        
        return if (allNonTriggeringPlayersFinished) {
            copy(gamePhase = GamePhase.ENDED)
        } else {
            this
        }
    }
    
    /**
     * Determines the winner (player with highest score).
     * 
     * @return The winning player, or null if game not ended
     */
    fun getWinner(): Player? {
        if (gamePhase != GamePhase.ENDED) {
            return null
        }
        return players.maxByOrNull { it.totalScore }
    }
    
    /**
     * Gets all players sorted by total score (descending).
     */
    fun getPlayersByScore(): List<Player> {
        return players.sortedByDescending { it.totalScore }
    }
    
    /**
     * Records a completed turn in the history.
     * 
     * The turn is recorded with the current round number. Round advancement
     * happens separately in [advanceToNextPlayer] when all players have played.
     * 
     * @param playerId The player who completed the turn
     * @param points Points earned (0 if busted/skipped)
     * @param outcome The outcome of the turn (SCORED, BUST, or SKIP)
     * @param previousScore The player's total score BEFORE this turn
     * @return Updated game with turn recorded
     */
    fun recordTurn(playerId: String, points: Int, outcome: TurnOutcome, previousScore: Int): Game {
        val record = TurnRecord(
            roundNumber = roundNumber,
            playerId = playerId,
            points = points,
            outcome = outcome,
            previousScore = previousScore
        )
        return copy(turnHistory = turnHistory + record)
    }
    
    /**
     * Removes the last turn from history (for undo functionality).
     * 
     * If the current round is ahead of the last recorded turn's round,
     * the round number is reverted to match the undone turn's round.
     * 
     * @return Updated game with last turn removed, or unchanged if no history
     */
    fun undoLastTurn(): Game {
        if (turnHistory.isEmpty()) {
            return this
        }
        
        val lastTurn = turnHistory.last()
        // If we've advanced to a new round, revert to the last turn's round
        val newRoundNumber = if (roundNumber > lastTurn.roundNumber) {
            lastTurn.roundNumber
        } else {
            roundNumber
        }
        
        return copy(
            turnHistory = turnHistory.dropLast(1),
            roundNumber = newRoundNumber
        )
    }
    
    /**
     * Gets the last recorded turn, if any.
     */
    fun getLastTurn(): TurnRecord? = turnHistory.lastOrNull()
}
