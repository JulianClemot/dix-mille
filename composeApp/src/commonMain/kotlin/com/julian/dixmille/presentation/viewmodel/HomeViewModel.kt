package com.julian.dixmille.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.domain.usecase.GetCurrentGameUseCase
import com.julian.dixmille.presentation.model.HomeEvent
import com.julian.dixmille.presentation.model.HomeUiState
import com.julian.dixmille.presentation.navigation.HomeNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * 
 * Responsibilities:
 * - Check if a saved game exists
 * - Provide game status summary for resume option
 * - Emit navigation events
 */
class HomeViewModel(
    private val getCurrentGameUseCase: GetCurrentGameUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    
    private val _navigationEvents = Channel<HomeNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    init {
        loadGameStatus()
    }
    
    /**
     * Handles user events from the Home screen.
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.NavigateToNewGame -> navigateToNewGame()
            is HomeEvent.NavigateToResumeGame -> navigateToResumeGame()
        }
    }
    
    /**
     * Loads the current game status to determine if resume option should be shown.
     */
    private fun loadGameStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getCurrentGameUseCase()
                .onSuccess { game ->
                    val isGameEnded = game.gamePhase == com.julian.dixmille.domain.model.GamePhase.ENDED
                    val hasGame = !isGameEnded
                    
                    val summary = if (hasGame) {
                        val playerCount = game.players.size
                        val roundNumber = game.roundNumber
                        val leader = game.players.maxByOrNull { it.totalScore }
                        val leaderInfo = if (leader != null && leader.totalScore > 0) {
                            "${leader.name}: ${leader.totalScore} pts"
                        } else {
                            "No scores yet"
                        }
                        "$playerCount players • Round $roundNumber • $leaderInfo"
                    } else null
                    
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasExistingGame = hasGame,
                            gameStatusSummary = summary
                        )
                    }
                }
                .onFailure {
                    // No game exists - that's fine
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasExistingGame = false,
                            gameStatusSummary = null
                        )
                    }
                }
        }
    }
    
    private fun navigateToNewGame() {
        viewModelScope.launch {
            _navigationEvents.send(HomeNavigationEvent.NavigateToGameSetup)
        }
    }
    
    private fun navigateToResumeGame() {
        viewModelScope.launch {
            _navigationEvents.send(HomeNavigationEvent.NavigateToScoreSheet)
        }
    }
}
