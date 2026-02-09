package com.julian.dixmille.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.usecase.GetCurrentGameUseCase
import com.julian.dixmille.presentation.model.GameEndEvent
import com.julian.dixmille.presentation.model.GameEndUiState
import com.julian.dixmille.presentation.navigation.GameEndNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Game End screen.
 * 
 * Responsibilities:
 * - Load final rankings from repository
 * - Handle new game action (clear game + navigate)
 * - Handle return home action (clear game + navigate)
 */
class GameEndViewModel(
    private val getCurrentGameUseCase: GetCurrentGameUseCase,
    private val gameRepository: GameRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(GameEndUiState())
    val state: StateFlow<GameEndUiState> = _state.asStateFlow()
    
    private val _navigationEvents = Channel<GameEndNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    init {
        loadFinalRankings()
    }
    
    /**
     * Handles user events from the Game End screen.
     */
    fun onEvent(event: GameEndEvent) {
        when (event) {
            is GameEndEvent.StartNewGame -> startNewGame()
            is GameEndEvent.ReturnHome -> returnHome()
        }
    }
    
    /**
     * Loads final rankings from the current game.
     */
    private fun loadFinalRankings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getCurrentGameUseCase()
                .onSuccess { game ->
                    val sortedPlayers = game.players.sortedByDescending { it.totalScore }
                    _state.update {
                        it.copy(
                            playersByScore = sortedPlayers,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            playersByScore = emptyList(),
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    private fun startNewGame() {
        viewModelScope.launch {
            // Clear the current game
            gameRepository.deleteGame()
            
            // Navigate to game setup
            _navigationEvents.send(GameEndNavigationEvent.NavigateToGameSetup)
        }
    }
    
    private fun returnHome() {
        viewModelScope.launch {
            // Clear the current game
            gameRepository.deleteGame()
            
            // Navigate to home
            _navigationEvents.send(GameEndNavigationEvent.NavigateToHome)
        }
    }
}
