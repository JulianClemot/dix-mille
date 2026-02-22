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
    val roundNumber: Int = 1,
    val rules: GameRules = GameRules()
) {
    init {
        require(players.size in rules.minPlayers..rules.maxPlayers) {
            "Game must have ${rules.minPlayers}-${rules.maxPlayers} players"
        }
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
            if (!rules.enableFinalRound) {
                return copy(gamePhase = GamePhase.ENDED)
            }
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

    /**
     * Resolves score collisions after a player scores.
     *
     * When a player scores and their new total equals another player's total,
     * that other player's score reverts to their previous score (before their
     * last scoring turn). This cascades: if the reverted score matches a third
     * player, that player also reverts, and so on.
     *
     * Collisions at score 0 are ignored.
     *
     * @param immunePlayerId The player who just scored (immune to collision)
     * @return Updated game with collisions resolved and COLLISION records added
     */
    fun resolveScoreCollisions(immunePlayerId: String): Game {
        var game = this
        val immunePlayerIds = mutableSetOf(immunePlayerId)
        val scoresToCheck = mutableSetOf<Int>()

        // Start by checking the scoring player's new score
        val scoringPlayer = game.players.find { it.id == immunePlayerId }
        if (scoringPlayer != null && scoringPlayer.totalScore > 0) {
            scoresToCheck.add(scoringPlayer.totalScore)
        }

        // Iteratively check for collisions (BFS approach)
        while (scoresToCheck.isNotEmpty()) {
            val scoreToCheck = scoresToCheck.first()
            scoresToCheck.remove(scoreToCheck)

            // Skip score 0
            if (scoreToCheck == 0) continue

            // Find all non-immune player IDs at this score
            val collidedPlayerIds = game.players
                .filter { player -> player.id !in immunePlayerIds && player.totalScore == scoreToCheck }
                .map { it.id }

            // Revert each collided player
            for (playerId in collidedPlayerIds) {
                // Get current player state from game
                val collidedPlayer = game.players.find { it.id == playerId } ?: continue

                // Find the last SCORED turn for this player where previousScore < totalScore
                val revertToScore = game.turnHistory
                    .filter { it.playerId == playerId && it.outcome == TurnOutcome.SCORED }
                    .lastOrNull { it.previousScore < collidedPlayer.totalScore }
                    ?.previousScore
                    ?: 0

                // Update the player's score
                val playerIndex = game.players.indexOfFirst { it.id == playerId }
                if (playerIndex != -1) {
                    val updatedPlayer = collidedPlayer.copy(totalScore = revertToScore)
                    val updatedPlayers = game.players.toMutableList()
                    updatedPlayers[playerIndex] = updatedPlayer
                    game = game.copy(players = updatedPlayers)

                    // Record the collision
                    game = game.recordTurn(
                        playerId = playerId,
                        points = 0,
                        outcome = TurnOutcome.COLLISION,
                        previousScore = scoreToCheck
                    )

                    // Mark this player as immune (can't be hit twice in same cascade)
                    immunePlayerIds.add(playerId)

                    // Queue the reverted score for cascade checking
                    if (revertToScore > 0) {
                        scoresToCheck.add(revertToScore)
                    }
                }
            }
        }

        return game
    }
}
