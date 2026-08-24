package com.julian.dixmille.feature.game_setup.presentation.model

import com.julian.dixmille.core.domain.model.SavedPlayer

data class GameSetupUiState(
    val selectedPlayers: List<SavedPlayer> = emptyList(),
    val allPlayers: List<SavedPlayer> = emptyList(),
    val unifiedInput: String = "",
    val targetScore: String = "10000",
    val error: String? = null,
    val quickAddError: String? = null,
    val isCreating: Boolean = false,
    val showPlayerSelector: Boolean = false,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 6,
) {
    val filteredPlayers: List<SavedPlayer>
        get() = if (unifiedInput.isBlank()) allPlayers
                else allPlayers.filter { it.name.value.contains(unifiedInput, ignoreCase = true) }

    val canStartGame: Boolean get() = selectedPlayers.size >= minPlayers
    val canAddMorePlayers: Boolean get() = selectedPlayers.size < maxPlayers
    val canConfirmSelection: Boolean get() = selectedPlayers.size >= minPlayers
    val canReorderPlayers: Boolean get() = selectedPlayers.size >= 2

    val canAddNewPlayer: Boolean
        get() {
            if (unifiedInput.isBlank()) return false
            if (!canAddMorePlayers) return false
            val trimmed = unifiedInput.trim()
            if (allPlayers.any { it.name.value.equals(trimmed, ignoreCase = true) }) return false
            return true
        }
}
