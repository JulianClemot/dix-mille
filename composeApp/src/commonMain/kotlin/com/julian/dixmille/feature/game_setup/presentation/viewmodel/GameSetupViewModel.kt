package com.julian.dixmille.feature.game_setup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.repository.GameRulesRepository
import com.julian.dixmille.core.presentation.navigation.GameSetupNavigationEvent
import com.julian.dixmille.feature.game_setup.domain.usecase.AddSavedPlayerUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.CreateGameUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.DeleteSavedPlayerUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.GetSavedPlayersUseCase
import com.julian.dixmille.feature.game_setup.presentation.model.GameSetupEvent
import com.julian.dixmille.feature.game_setup.presentation.model.GameSetupUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameSetupViewModel(
    private val createGameUseCase: CreateGameUseCase,
    private val gameRulesRepository: GameRulesRepository,
    private val getSavedPlayersUseCase: GetSavedPlayersUseCase,
    private val addSavedPlayerUseCase: AddSavedPlayerUseCase,
    private val deleteSavedPlayerUseCase: DeleteSavedPlayerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GameSetupUiState())
    val state: StateFlow<GameSetupUiState> = _state.asStateFlow()

    private val _navigationEvents = Channel<GameSetupNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        loadRules()
        loadPlayers()
    }

    private fun loadRules() {
        viewModelScope.launch {
            val rules = gameRulesRepository.getRules().getOrElse { GameRules.DEFAULT }
            _state.update {
                it.copy(
                    targetScore = rules.targetScore.value.toString(),
                    minPlayers = rules.minPlayers,
                    maxPlayers = rules.maxPlayers,
                )
            }
        }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            val players = getSavedPlayersUseCase()
            _state.update { it.copy(allPlayers = players) }
        }
    }

    fun refreshRules() {
        loadRules()
    }

    fun onEvent(event: GameSetupEvent) {
        when (event) {
            is GameSetupEvent.ShowPlayerSelector -> _state.update { it.copy(showPlayerSelector = true) }
            is GameSetupEvent.HidePlayerSelector -> _state.update {
                it.copy(showPlayerSelector = false, unifiedInput = "")
            }
            is GameSetupEvent.SelectPlayer -> selectPlayer(event.player)
            is GameSetupEvent.DeselectPlayer -> _state.update { s ->
                s.copy(selectedPlayers = s.selectedPlayers.filter { it.id.value != event.playerId })
            }
            is GameSetupEvent.DeleteSavedPlayer -> deleteSavedPlayer(event.playerId)
            is GameSetupEvent.ConfirmPlayerSelection -> _state.update {
                it.copy(
                    selectedPlayers = event.selectedPlayers,
                    showPlayerSelector = false,
                    unifiedInput = "",
                )
            }
            is GameSetupEvent.RemoveSelectedPlayer -> _state.update { s ->
                s.copy(selectedPlayers = s.selectedPlayers.filter { it.id.value != event.playerId })
            }
            is GameSetupEvent.MovePlayer -> movePlayer(event.fromIndex, event.toIndex)
            is GameSetupEvent.UpdateUnifiedInput -> _state.update { it.copy(unifiedInput = event.input) }
            is GameSetupEvent.QuickAddPlayer -> quickAddPlayer(event.name)
            is GameSetupEvent.UpdateTargetScore -> updateTargetScore(event.score)
            is GameSetupEvent.CreateGame -> createGame()
        }
    }

    private fun selectPlayer(player: SavedPlayer) {
        _state.update { s ->
            if (s.selectedPlayers.size < s.maxPlayers) {
                val updated = s.selectedPlayers + player
                s.copy(selectedPlayers = updated)
            } else {
                s
            }
        }
    }

    private fun movePlayer(fromIndex: Int, toIndex: Int) {
        _state.update { s ->
            val players = s.selectedPlayers
            if (fromIndex == toIndex ||
                fromIndex !in players.indices ||
                toIndex !in players.indices
            ) {
                s
            } else {
                val mutable = players.toMutableList()
                val player = mutable.removeAt(fromIndex)
                mutable.add(toIndex, player)
                s.copy(selectedPlayers = mutable)
            }
        }
    }

    private fun quickAddPlayer(name: String) {
        viewModelScope.launch {
            addSavedPlayerUseCase(name)
                .onSuccess { player ->
                    _state.update { s ->
                        val updatedAll = (s.allPlayers + player).sortedBy { it.name.value.lowercase() }
                        val updatedSelected = if (s.selectedPlayers.size < s.maxPlayers) {
                            s.selectedPlayers + player
                        } else {
                            s.selectedPlayers
                        }
                        s.copy(
                            allPlayers = updatedAll,
                            selectedPlayers = updatedSelected,
                            unifiedInput = "",
                            quickAddError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(quickAddError = error.message ?: "Failed to add player") }
                }
        }
    }

    private fun deleteSavedPlayer(playerId: String) {
        viewModelScope.launch {
            deleteSavedPlayerUseCase(playerId)
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            allPlayers = s.allPlayers.filter { it.id.value != playerId },
                            selectedPlayers = s.selectedPlayers.filter { it.id.value != playerId },
                            deleteErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(deleteErrorMessage = error.message ?: "Failed to delete player") }
                }
        }
    }

    private fun updateTargetScore(score: String) {
        _state.update { it.copy(targetScore = score, error = null) }
    }

    private fun createGame() {
        val currentState = _state.value
        val target = currentState.targetScore.toIntOrNull()

        when {
            !currentState.canStartGame -> {
                _state.update { it.copy(error = "Need at least ${currentState.minPlayers} players") }
                return
            }
            target == null || target <= 0 -> {
                _state.update { it.copy(error = "Invalid target score") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            createGameUseCase(currentState.selectedPlayers, target)
                .onSuccess {
                    _state.update { it.copy(isCreating = false) }
                    _navigationEvents.send(GameSetupNavigationEvent.NavigateToScoreSheet)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isCreating = false, error = error.message ?: "Failed to create game")
                    }
                }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            _navigationEvents.send(GameSetupNavigationEvent.NavigateBack)
        }
    }

    fun navigateToRulesSettings() {
        viewModelScope.launch {
            _navigationEvents.send(GameSetupNavigationEvent.NavigateToRulesSettings)
        }
    }
}
