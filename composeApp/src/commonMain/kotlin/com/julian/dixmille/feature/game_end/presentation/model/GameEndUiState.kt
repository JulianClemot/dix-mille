package com.julian.dixmille.feature.game_end.presentation.model

import com.julian.dixmille.core.domain.model.Player

/**
 * UI state for the Game End screen.
 */
data class GameEndUiState(
    val playersByScore: List<Player> = emptyList(),
    val isLoading: Boolean = true
)
