package com.julian.dixmille.presentation.model

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasExistingGame: Boolean = false,
    val gameStatusSummary: String? = null
)
