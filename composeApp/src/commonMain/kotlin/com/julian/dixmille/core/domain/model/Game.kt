package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.event.DomainEvent
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore

/**
 * Represents a player with their computed rank in the final game standings.
 *
 * Rank is 1-based. Players with equal scores below the target share the same rank
 * (standard competition ranking: 1, 2, 2, 4).
 */
data class RankedPlayer(
    val player: Player,
    val rank: Int
)
/**
 * Represents a complete Dix Mille game.
 */
data class Game(
    val id: GameId,
    val players: List<Player>,
    val targetScore: TargetScore = TargetScore.DEFAULT,
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.IN_PROGRESS,
    val triggeringPlayerId: PlayerId? = null,
    val createdAt: Long,
    val turnHistory: List<TurnRecord> = emptyList(),
    val roundNumber: Int = 1,
    val rules: GameRules = GameRules()
) {
    init {
        require(players.size in rules.minPlayers..rules.maxPlayers) {
            "Game must have ${rules.minPlayers}-${rules.maxPlayers} players"
        }
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
     * @return GameResult with updated game and emitted domain events
     */
    fun checkAndTriggerFinalRound(): GameResult {
        if (gamePhase != GamePhase.IN_PROGRESS) {
            return GameResult(game = this, events = emptyList())
        }

        if (currentPlayer.totalScore.value >= targetScore.value) {
            if (!rules.enableFinalRound) {
                val endedGame = copy(gamePhase = GamePhase.ENDED)
                return GameResult(
                    game = endedGame,
                    events = listOf(DomainEvent.GameEnded(winnerId = currentPlayer.id))
                )
            }
            val finalRoundGame = copy(
                gamePhase = GamePhase.FINAL_ROUND,
                triggeringPlayerId = currentPlayer.id
            )
            return GameResult(
                game = finalRoundGame,
                events = listOf(DomainEvent.FinalRoundStarted(triggeringPlayerId = currentPlayer.id))
            )
        }

        return GameResult(game = this, events = emptyList())
    }

    /**
     * Checks if the game should end (all non-triggering players have played final round).
     *
     * @return GameResult with updated game and emitted domain events
     */
    fun checkAndEndGame(): GameResult {
        if (gamePhase != GamePhase.FINAL_ROUND) {
            return GameResult(game = this, events = emptyList())
        }

        val allNonTriggeringPlayersFinished = players
            .filter { it.id != triggeringPlayerId }
            .all { it.hasPlayedFinalRound }

        return if (allNonTriggeringPlayersFinished) {
            val endedGame = copy(gamePhase = GamePhase.ENDED)
            GameResult(
                game = endedGame,
                events = listOf(DomainEvent.GameEnded(winnerId = endedGame.getWinner()?.id ?: PlayerId("unknown")))
            )
        } else {
            GameResult(game = this, events = emptyList())
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
     * Computes the final game ranking.
     *
     * Players who reached the target score come first, ordered by when they first
     * crossed the target in [turnHistory] (lower index = earlier = higher rank).
     * Players below the target follow, sorted by [Player.totalScore] descending.
     *
     * Standard competition ranking is used: tied players below the target share
     * the same rank number and the next distinct score skips the corresponding
     * rank numbers (e.g. 1, 2, 2, 4).
     *
     * @return Ordered list of [RankedPlayer] from rank 1 downward.
     */
    fun getRanking(): List<RankedPlayer> {
        val (atTarget, belowTarget) = players.partition { it.totalScore.value >= targetScore.value }

        val sortedAtTarget = atTarget.sortedBy { player ->
            val crossingIndex = turnHistory.indexOfFirst { record ->
                record.playerId == player.id &&
                    record.outcome == TurnOutcome.SCORED &&
                    record.previousScore.value + record.points.value >= targetScore.value
            }
            if (crossingIndex >= 0) crossingIndex else Int.MAX_VALUE
        }

        val sortedBelow = belowTarget.sortedByDescending { it.totalScore }

        val allSorted = sortedAtTarget + sortedBelow
        val result = mutableListOf<RankedPlayer>()
        var currentRank = 1

        allSorted.forEachIndexed { index, player ->
            if (index == 0) {
                result.add(RankedPlayer(player, currentRank))
            } else {
                val prev = allSorted[index - 1]
                val isTie = player in sortedBelow &&
                    prev in sortedBelow &&
                    player.totalScore == prev.totalScore
                if (!isTie) currentRank = index + 1
                result.add(RankedPlayer(player, currentRank))
            }
        }

        return result
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
    fun recordTurn(playerId: PlayerId, points: Score, outcome: TurnOutcome, previousScore: Score): Game {
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
     * When undoing in FINAL_ROUND:
     * - If the undone turn belongs to the triggering player (the turn that crossed the
     *   target threshold), the phase reverts to IN_PROGRESS and triggeringPlayerId is
     *   cleared — unless another player also has a score >= target, in which case
     *   FINAL_ROUND is kept.
     * - If the undone turn belongs to any other player, hasPlayedFinalRound is reset
     *   for that player and the phase stays FINAL_ROUND.
     *
     * @return Updated game with last turn removed
     * @throws IllegalStateException if turn history is empty
     */
    fun undoLastTurn(): Game {
        check(turnHistory.isNotEmpty()) { "No turns to undo" }

        val lastTurn = turnHistory.last()
        // If we've advanced to a new round, revert to the last turn's round
        val newRoundNumber = if (roundNumber > lastTurn.roundNumber) {
            lastTurn.roundNumber
        } else {
            roundNumber
        }

        var updatedGame = copy(
            turnHistory = turnHistory.dropLast(1),
            roundNumber = newRoundNumber,
        )

        // Handle final round phase reversion
        if (gamePhase == GamePhase.FINAL_ROUND && triggeringPlayerId != null) {
            val isTriggeringTurn = lastTurn.playerId == triggeringPlayerId
                && lastTurn.previousScore.value < targetScore.value
                && (lastTurn.previousScore.value + lastTurn.points.value) >= targetScore.value

            if (isTriggeringTurn) {
                val otherPlayerAtTarget = players.any {
                    it.id != triggeringPlayerId && it.totalScore.value >= targetScore.value
                }
                updatedGame = if (otherPlayerAtTarget) {
                    updatedGame // Keep FINAL_ROUND and triggeringPlayerId unchanged
                } else {
                    updatedGame.copy(
                        gamePhase = GamePhase.IN_PROGRESS,
                        triggeringPlayerId = null,
                    )
                }
            } else {
                // Non-triggering player's turn was undone — reset their hasPlayedFinalRound
                val playerIndex = updatedGame.players.indexOfFirst { it.id == lastTurn.playerId }
                if (playerIndex != -1) {
                    val updatedPlayers = updatedGame.players.toMutableList()
                    updatedPlayers[playerIndex] = updatedPlayers[playerIndex].copy(hasPlayedFinalRound = false)
                    updatedGame = updatedGame.copy(players = updatedPlayers)
                }
            }
        }

        return updatedGame
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
        val scoringPlayer = game.players.find { it.id.value == immunePlayerId }
        if (scoringPlayer != null && scoringPlayer.totalScore.value > 0) {
            scoresToCheck.add(scoringPlayer.totalScore.value)
        }

        // Iteratively check for collisions (BFS approach)
        while (scoresToCheck.isNotEmpty()) {
            val scoreToCheck = scoresToCheck.first()
            scoresToCheck.remove(scoreToCheck)

            // Skip score 0
            if (scoreToCheck == 0) continue

            // Find all non-immune player IDs at this score
            val collidedPlayerIds = game.players
                .filter { player -> player.id.value !in immunePlayerIds && player.totalScore.value == scoreToCheck }
                .map { it.id.value }

            // Revert each collided player
            for (playerId in collidedPlayerIds) {
                // Get current player state from game
                val collidedPlayer = game.players.find { it.id.value == playerId } ?: continue

                // Find the last SCORED turn for this player where previousScore < totalScore
                val revertToScore = game.turnHistory
                    .filter { it.playerId.value == playerId && it.outcome == TurnOutcome.SCORED }
                    .lastOrNull { it.previousScore.value < collidedPlayer.totalScore.value }
                    ?.previousScore?.value
                    ?: 0

                // Update the player's score
                val playerIndex = game.players.indexOfFirst { it.id.value == playerId }
                if (playerIndex != -1) {
                    val updatedPlayer = collidedPlayer.copy(totalScore = Score(revertToScore))
                    val updatedPlayers = game.players.toMutableList()
                    updatedPlayers[playerIndex] = updatedPlayer
                    game = game.copy(players = updatedPlayers)

                    // Record the collision
                    game = game.recordTurn(
                        playerId = PlayerId(playerId),
                        points = Score.ZERO,
                        outcome = TurnOutcome.COLLISION,
                        previousScore = Score(scoreToCheck)
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
