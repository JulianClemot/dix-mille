package com.julian.dixmille.feature.game_end.presentation.model

import com.julian.dixmille.core.domain.model.RankedPlayer

/**
 * UI state for the Game End screen.
 */
data class GameEndUiState(
    val rankedPlayers: List<RankedPlayer> = emptyList(),
    val isLoading: Boolean = true
)
