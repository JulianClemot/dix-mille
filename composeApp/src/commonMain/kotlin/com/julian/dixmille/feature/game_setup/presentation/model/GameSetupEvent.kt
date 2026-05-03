package com.julian.dixmille.feature.game_setup.presentation.model

import com.julian.dixmille.core.domain.model.SavedPlayer

sealed class GameSetupEvent {
    data object ShowPlayerSelector : GameSetupEvent()
    data object HidePlayerSelector : GameSetupEvent()
    data class SelectPlayer(val player: SavedPlayer) : GameSetupEvent()
    data class DeselectPlayer(val playerId: String) : GameSetupEvent()
    data class ConfirmPlayerSelection(val selectedPlayers: List<SavedPlayer>) : GameSetupEvent()
    data class RemoveSelectedPlayer(val playerId: String) : GameSetupEvent()
    data class UpdateUnifiedInput(val input: String) : GameSetupEvent()
    data class QuickAddPlayer(val name: String) : GameSetupEvent()
    data class UpdateTargetScore(val score: String) : GameSetupEvent()
    data object CreateGame : GameSetupEvent()
}
