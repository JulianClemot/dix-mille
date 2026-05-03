package com.julian.dixmille.feature.game_setup.presentation.model

import com.julian.dixmille.core.domain.model.SavedPlayer

data class GameSetupUiState(
    val selectedPlayers: List<SavedPlayer> = emptyList(),
    val allPlayers: List<SavedPlayer> = emptyList(),
    val searchQuery: String = "",
    val playerNameInput: String = "",
    val targetScore: String = "10000",
    val error: String? = null,
    val quickAddError: String? = null,
    val isCreating: Boolean = false,
    val showPlayerSelector: Boolean = false,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 6,
) {
    val filteredPlayers: List<SavedPlayer>
        get() = if (searchQuery.isBlank()) allPlayers
                else allPlayers.filter { it.name.value.contains(searchQuery, ignoreCase = true) }

    val canStartGame: Boolean get() = selectedPlayers.size >= minPlayers
    val canAddMorePlayers: Boolean get() = selectedPlayers.size < maxPlayers
    val canConfirmSelection: Boolean get() = selectedPlayers.size >= minPlayers
}
