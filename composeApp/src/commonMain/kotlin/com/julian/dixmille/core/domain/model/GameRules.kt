package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.TargetScore

data class GameRules(
    val targetScore: TargetScore = TargetScore.DEFAULT,
    val entryMinimumScore: EntryMinimumScore = EntryMinimumScore.DEFAULT,
    val consecutiveBustsForPenalty: Int = DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY,
    val minPlayers: Int = DEFAULT_MIN_PLAYERS,
    val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
    val enableBustPenalty: Boolean = DEFAULT_ENABLE_BUST_PENALTY,
    val enableFinalRound: Boolean = DEFAULT_ENABLE_FINAL_ROUND
) {
    init {
        require(minPlayers >= 2) { "Minimum players must be at least 2" }
        require(maxPlayers >= minPlayers) { "Maximum players must be >= minimum players" }
        require(maxPlayers <= 10) { "Maximum players must be at most 10" }
        require(consecutiveBustsForPenalty >= 2) { "Consecutive busts for penalty must be at least 2" }
    }

    companion object {
        const val DEFAULT_TARGET_SCORE: Int = 10_000
        const val DEFAULT_ENTRY_MINIMUM_SCORE: Int = 500
        const val DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY: Int = 3
        const val DEFAULT_MIN_PLAYERS: Int = 2
        const val DEFAULT_MAX_PLAYERS: Int = 6
        const val DEFAULT_ENABLE_BUST_PENALTY: Boolean = true
        const val DEFAULT_ENABLE_FINAL_ROUND: Boolean = true

        val DEFAULT: GameRules = GameRules()
    }
}
