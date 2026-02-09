package com.julian.dixmille.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.domain.usecase.*
import com.julian.dixmille.presentation.model.ScoreSheetEvent
import com.julian.dixmille.presentation.model.ScoreSheetUiState
import com.julian.dixmille.presentation.navigation.ScoreSheetNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Score Sheet screen (active game play).
 * 
 * Responsibilities:
 * - Load and manage active game state
 * - Handle score entry, bust, skip, undo operations
 * - Detect game end and emit navigation event
 * - Manage bust confirmation dialog
 */
class ScoreSheetViewModel(
    private val getCurrentGameUseCase: GetCurrentGameUseCase,
    private val addScoreEntryUseCase: AddScoreEntryUseCase,
    private val commitTurnUseCase: CommitTurnUseCase,
    private val bustTurnUseCase: BustTurnUseCase,
    private val skipTurnUseCase: SkipTurnUseCase,
    private val undoLastEntryUseCase: UndoLastEntryUseCase,
    private val undoLastTurnUseCase: UndoLastTurnUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ScoreSheetUiState())
    val state: StateFlow<ScoreSheetUiState> = _state.asStateFlow()
    
    private val _navigationEvents = Channel<ScoreSheetNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    init {
        loadGame()
    }
    
    /**
     * Handles user events from the Score Sheet screen.
     */
    fun onEvent(event: ScoreSheetEvent) {
        when (event) {
            is ScoreSheetEvent.AddScore -> addScore(event.points, event.isPreset, event.label)
            is ScoreSheetEvent.UndoLastEntry -> undoLastEntry()
            is ScoreSheetEvent.UndoLastTurn -> undoLastTurn()
            is ScoreSheetEvent.BustTurn -> bustTurn()
            is ScoreSheetEvent.SkipTurn -> skipTurn()
            is ScoreSheetEvent.ShowBustDialog -> showBustDialog()
            is ScoreSheetEvent.HideBustDialog -> hideBustDialog()
            is ScoreSheetEvent.DismissError -> dismissError()
            is ScoreSheetEvent.NavigateBack -> navigateBack()
        }
    }
    
    private fun loadGame() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getCurrentGameUseCase()
                .onSuccess { game ->
                    _state.update {
                        it.copy(
                            game = game,
                            isLoading = false,
                            error = null
                        )
                    }
                    
                    // Check if game just ended and emit navigation event
                    if (game.gamePhase == com.julian.dixmille.domain.model.GamePhase.ENDED) {
                        val winner = game.players.maxByOrNull { it.totalScore }
                        if (winner != null) {
                            _navigationEvents.send(
                                ScoreSheetNavigationEvent.NavigateToGameEnd(
                                    winnerName = winner.name,
                                    winnerScore = winner.totalScore
                                )
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            game = null,
                            isLoading = false,
                            error = error.message ?: "Failed to load game"
                        )
                    }
                }
        }
    }
    
    private fun addScore(points: Int, isPreset: Boolean, label: String?) {
        viewModelScope.launch {
            val currentPlayer = _state.value.currentPlayer
            
            // Entry validation: Must have 500+ points to enter game
            if (currentPlayer != null && !currentPlayer.hasEnteredGame && points < 500) {
                _state.update {
                    it.copy(error = "Need at least 500 points to enter the game")
                }
                return@launch
            }
            
            // Add score entry
            addScoreEntryUseCase(points, isPreset, label)
                .onSuccess {
                    // Auto-commit the turn immediately
                    commitTurnUseCase()
                        .onSuccess {
                            loadGame()
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(error = error.message ?: "Failed to commit turn")
                            }
                        }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to add score")
                    }
                }
        }
    }
    
    private fun undoLastEntry() {
        viewModelScope.launch {
            undoLastEntryUseCase()
                .onSuccess {
                    loadGame()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to undo")
                    }
                }
        }
    }
    
    private fun undoLastTurn() {
        viewModelScope.launch {
            undoLastTurnUseCase()
                .onSuccess {
                    loadGame()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to undo turn")
                    }
                }
        }
    }
    
    private fun bustTurn() {
        viewModelScope.launch {
            _state.update { it.copy(showBustDialog = false) }
            
            bustTurnUseCase()
                .onSuccess {
                    loadGame()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to bust turn")
                    }
                }
        }
    }
    
    private fun skipTurn() {
        viewModelScope.launch {
            skipTurnUseCase()
                .onSuccess {
                    loadGame()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to skip turn")
                    }
                }
        }
    }
    
    private fun showBustDialog() {
        _state.update { it.copy(showBustDialog = true) }
    }
    
    private fun hideBustDialog() {
        _state.update { it.copy(showBustDialog = false) }
    }
    
    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvents.send(ScoreSheetNavigationEvent.NavigateBack)
        }
    }
}
