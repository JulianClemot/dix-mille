package com.julian.dixmille.presentation.model

/**
 * UI state for the Game Setup screen.
 */
data class GameSetupUiState(
    val playerNames: List<String> = listOf("", ""),
    val targetScore: String = "10000",
    val error: String? = null,
    val isCreating: Boolean = false,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 6
)
