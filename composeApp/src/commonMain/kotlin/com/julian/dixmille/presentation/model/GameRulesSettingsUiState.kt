package com.julian.dixmille.presentation.model

import com.julian.dixmille.domain.model.GameRules

data class GameRulesSettingsUiState(
    val targetScore: String = GameRules.DEFAULT_TARGET_SCORE.toString(),
    val entryMinimumScore: String = GameRules.DEFAULT_ENTRY_MINIMUM_SCORE.toString(),
    val consecutiveBustsForPenalty: String = GameRules.DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY.toString(),
    val minPlayers: String = GameRules.DEFAULT_MIN_PLAYERS.toString(),
    val maxPlayers: String = GameRules.DEFAULT_MAX_PLAYERS.toString(),
    val enableBustPenalty: Boolean = GameRules.DEFAULT_ENABLE_BUST_PENALTY,
    val enableFinalRound: Boolean = GameRules.DEFAULT_ENABLE_FINAL_ROUND,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)
