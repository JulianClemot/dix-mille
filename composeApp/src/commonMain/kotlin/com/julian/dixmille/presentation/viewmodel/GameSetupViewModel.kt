package com.julian.dixmille.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.domain.usecase.CreateGameUseCase
import com.julian.dixmille.presentation.model.GameSetupEvent
import com.julian.dixmille.presentation.model.GameSetupUiState
import com.julian.dixmille.presentation.navigation.GameSetupNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Game Setup screen.
 * 
 * Responsibilities:
 * - Manage game setup form state (player names, target score)
 * - Validate input
 * - Create new game via CreateGameUseCase
 * - Emit navigation events
 */
class GameSetupViewModel(
    private val createGameUseCase: CreateGameUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(GameSetupUiState())
    val state: StateFlow<GameSetupUiState> = _state.asStateFlow()
    
    private val _navigationEvents = Channel<GameSetupNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    /**
     * Handles user events from the Game Setup screen.
     */
    fun onEvent(event: GameSetupEvent) {
        when (event) {
            is GameSetupEvent.UpdatePlayerName -> updatePlayerName(event.index, event.name)
            is GameSetupEvent.AddPlayer -> addPlayer()
            is GameSetupEvent.RemovePlayer -> removePlayer(event.index)
            is GameSetupEvent.UpdateTargetScore -> updateTargetScore(event.score)
            is GameSetupEvent.CreateGame -> createGame()
        }
    }
    
    private fun updatePlayerName(index: Int, name: String) {
        _state.update { currentState ->
            val updatedNames = currentState.playerNames.toMutableList().apply {
                if (index in indices) {
                    this[index] = name
                }
            }
            currentState.copy(
                playerNames = updatedNames,
                error = null
            )
        }
    }
    
    private fun addPlayer() {
        _state.update { currentState ->
            if (currentState.playerNames.size < 6) {
                currentState.copy(
                    playerNames = currentState.playerNames + ""
                )
            } else {
                currentState
            }
        }
    }
    
    private fun removePlayer(index: Int) {
        _state.update { currentState ->
            if (currentState.playerNames.size > 2 && index in currentState.playerNames.indices) {
                currentState.copy(
                    playerNames = currentState.playerNames.toMutableList().apply {
                        removeAt(index)
                    }
                )
            } else {
                currentState
            }
        }
    }
    
    private fun updateTargetScore(score: String) {
        _state.update {
            it.copy(
                targetScore = score,
                error = null
            )
        }
    }
    
    private fun createGame() {
        val currentState = _state.value
        val names = currentState.playerNames.map { it.trim() }.filter { it.isNotBlank() }
        val target = currentState.targetScore.toIntOrNull()
        
        // Validation
        when {
            names.size < 2 -> {
                _state.update { it.copy(error = "Need at least 2 players") }
                return
            }
            names.size > 6 -> {
                _state.update { it.copy(error = "Maximum 6 players") }
                return
            }
            target == null || target <= 0 -> {
                _state.update { it.copy(error = "Invalid target score") }
                return
            }
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            
            createGameUseCase(names, target)
                .onSuccess {
                    _state.update { it.copy(isCreating = false) }
                    _navigationEvents.send(GameSetupNavigationEvent.NavigateToScoreSheet)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create game"
                        )
                    }
                }
        }
    }
    
    fun navigateBack() {
        viewModelScope.launch {
            _navigationEvents.send(GameSetupNavigationEvent.NavigateBack)
        }
    }
}
