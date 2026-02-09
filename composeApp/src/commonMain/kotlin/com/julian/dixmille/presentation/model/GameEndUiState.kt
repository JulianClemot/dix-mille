package com.julian.dixmille.presentation.model

import com.julian.dixmille.domain.model.Player

/**
 * UI state for the Game End screen.
 */
data class GameEndUiState(
    val playersByScore: List<Player> = emptyList(),
    val isLoading: Boolean = true
)
