package com.julian.dixmille.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.domain.repository.GameRulesRepository
import com.julian.dixmille.presentation.model.GameRulesSettingsEvent
import com.julian.dixmille.presentation.model.GameRulesSettingsUiState
import com.julian.dixmille.presentation.navigation.GameRulesSettingsNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameRulesSettingsViewModel(
    private val gameRulesRepository: GameRulesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameRulesSettingsUiState())
    val state: StateFlow<GameRulesSettingsUiState> = _state.asStateFlow()

    private val _navigationEvents = Channel<GameRulesSettingsNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var savedRules: GameRules = GameRules.DEFAULT

    init {
        loadRules()
    }

    fun onEvent(event: GameRulesSettingsEvent) {
        when (event) {
            is GameRulesSettingsEvent.UpdateTargetScore -> updateField { it.copy(targetScore = event.value) }
            is GameRulesSettingsEvent.UpdateEntryMinimumScore -> updateField { it.copy(entryMinimumScore = event.value) }
            is GameRulesSettingsEvent.UpdateConsecutiveBustsForPenalty -> updateField { it.copy(consecutiveBustsForPenalty = event.value) }
            is GameRulesSettingsEvent.UpdateMinPlayers -> updateField { it.copy(minPlayers = event.value) }
            is GameRulesSettingsEvent.UpdateMaxPlayers -> updateField { it.copy(maxPlayers = event.value) }
            is GameRulesSettingsEvent.UpdateEnableBustPenalty -> updateField { it.copy(enableBustPenalty = event.enabled) }
            is GameRulesSettingsEvent.UpdateEnableFinalRound -> updateField { it.copy(enableFinalRound = event.enabled) }
            is GameRulesSettingsEvent.Save -> save()
            is GameRulesSettingsEvent.ResetAll -> resetAll()
            is GameRulesSettingsEvent.ResetTargetScore -> updateField { it.copy(targetScore = GameRules.DEFAULT_TARGET_SCORE.toString()) }
            is GameRulesSettingsEvent.ResetEntryMinimumScore -> updateField { it.copy(entryMinimumScore = GameRules.DEFAULT_ENTRY_MINIMUM_SCORE.toString()) }
            is GameRulesSettingsEvent.ResetConsecutiveBustsForPenalty -> updateField { it.copy(consecutiveBustsForPenalty = GameRules.DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY.toString()) }
            is GameRulesSettingsEvent.ResetMinPlayers -> updateField { it.copy(minPlayers = GameRules.DEFAULT_MIN_PLAYERS.toString()) }
            is GameRulesSettingsEvent.ResetMaxPlayers -> updateField { it.copy(maxPlayers = GameRules.DEFAULT_MAX_PLAYERS.toString()) }
            is GameRulesSettingsEvent.NavigateBack -> handleBack()
            is GameRulesSettingsEvent.ConfirmDiscard -> confirmDiscard()
            is GameRulesSettingsEvent.DismissDiscardDialog -> _state.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun loadRules() {
        viewModelScope.launch {
            val rules = gameRulesRepository.getRules().getOrElse { GameRules.DEFAULT }
            savedRules = rules
            _state.update {
                it.copy(
                    targetScore = rules.targetScore.toString(),
                    entryMinimumScore = rules.entryMinimumScore.toString(),
                    consecutiveBustsForPenalty = rules.consecutiveBustsForPenalty.toString(),
                    minPlayers = rules.minPlayers.toString(),
                    maxPlayers = rules.maxPlayers.toString(),
                    enableBustPenalty = rules.enableBustPenalty,
                    enableFinalRound = rules.enableFinalRound,
                    hasUnsavedChanges = false,
                    isLoading = false
                )
            }
        }
    }

    private fun updateField(transform: (GameRulesSettingsUiState) -> GameRulesSettingsUiState) {
        _state.update { current ->
            val updated = transform(current)
            updated.copy(
                hasUnsavedChanges = hasChanges(updated),
                error = null
            )
        }
    }

    private fun hasChanges(state: GameRulesSettingsUiState): Boolean {
        return state.targetScore != savedRules.targetScore.toString() ||
            state.entryMinimumScore != savedRules.entryMinimumScore.toString() ||
            state.consecutiveBustsForPenalty != savedRules.consecutiveBustsForPenalty.toString() ||
            state.minPlayers != savedRules.minPlayers.toString() ||
            state.maxPlayers != savedRules.maxPlayers.toString() ||
            state.enableBustPenalty != savedRules.enableBustPenalty ||
            state.enableFinalRound != savedRules.enableFinalRound
    }

    private fun save() {
        val current = _state.value
        val targetScore = current.targetScore.toIntOrNull()
        val entryMinimum = current.entryMinimumScore.toIntOrNull()
        val bustsForPenalty = current.consecutiveBustsForPenalty.toIntOrNull()
        val minPlayers = current.minPlayers.toIntOrNull()
        val maxPlayers = current.maxPlayers.toIntOrNull()

        // Validate all fields are valid integers
        if (targetScore == null || entryMinimum == null || bustsForPenalty == null ||
            minPlayers == null || maxPlayers == null
        ) {
            _state.update { it.copy(error = "All numeric fields must be valid numbers") }
            return
        }

        // Validate constraints
        val rules = try {
            GameRules(
                targetScore = targetScore,
                entryMinimumScore = entryMinimum,
                consecutiveBustsForPenalty = bustsForPenalty,
                minPlayers = minPlayers,
                maxPlayers = maxPlayers,
                enableBustPenalty = current.enableBustPenalty,
                enableFinalRound = current.enableFinalRound
            )
        } catch (e: IllegalArgumentException) {
            _state.update { it.copy(error = e.message) }
            return
        }

        viewModelScope.launch {
            gameRulesRepository.saveRules(rules)
                .onSuccess {
                    savedRules = rules
                    _state.update { it.copy(hasUnsavedChanges = false, error = null) }
                    _navigationEvents.send(GameRulesSettingsNavigationEvent.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Failed to save rules") }
                }
        }
    }

    private fun resetAll() {
        _state.update {
            it.copy(
                targetScore = GameRules.DEFAULT_TARGET_SCORE.toString(),
                entryMinimumScore = GameRules.DEFAULT_ENTRY_MINIMUM_SCORE.toString(),
                consecutiveBustsForPenalty = GameRules.DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY.toString(),
                minPlayers = GameRules.DEFAULT_MIN_PLAYERS.toString(),
                maxPlayers = GameRules.DEFAULT_MAX_PLAYERS.toString(),
                enableBustPenalty = GameRules.DEFAULT_ENABLE_BUST_PENALTY,
                enableFinalRound = GameRules.DEFAULT_ENABLE_FINAL_ROUND,
                hasUnsavedChanges = hasChanges(
                    it.copy(
                        targetScore = GameRules.DEFAULT_TARGET_SCORE.toString(),
                        entryMinimumScore = GameRules.DEFAULT_ENTRY_MINIMUM_SCORE.toString(),
                        consecutiveBustsForPenalty = GameRules.DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY.toString(),
                        minPlayers = GameRules.DEFAULT_MIN_PLAYERS.toString(),
                        maxPlayers = GameRules.DEFAULT_MAX_PLAYERS.toString(),
                        enableBustPenalty = GameRules.DEFAULT_ENABLE_BUST_PENALTY,
                        enableFinalRound = GameRules.DEFAULT_ENABLE_FINAL_ROUND
                    )
                ),
                error = null
            )
        }
    }

    private fun handleBack() {
        if (_state.value.hasUnsavedChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch {
                _navigationEvents.send(GameRulesSettingsNavigationEvent.NavigateBack)
            }
        }
    }

    private fun confirmDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch {
            _navigationEvents.send(GameRulesSettingsNavigationEvent.NavigateBack)
        }
    }
}
