package com.julian.dixmille.presentation.model

sealed class GameRulesSettingsEvent {
    data class UpdateTargetScore(val value: String) : GameRulesSettingsEvent()
    data class UpdateEntryMinimumScore(val value: String) : GameRulesSettingsEvent()
    data class UpdateConsecutiveBustsForPenalty(val value: String) : GameRulesSettingsEvent()
    data class UpdateMinPlayers(val value: String) : GameRulesSettingsEvent()
    data class UpdateMaxPlayers(val value: String) : GameRulesSettingsEvent()
    data class UpdateEnableBustPenalty(val enabled: Boolean) : GameRulesSettingsEvent()
    data class UpdateEnableFinalRound(val enabled: Boolean) : GameRulesSettingsEvent()
    data object Save : GameRulesSettingsEvent()
    data object ResetAll : GameRulesSettingsEvent()
    data object ResetTargetScore : GameRulesSettingsEvent()
    data object ResetEntryMinimumScore : GameRulesSettingsEvent()
    data object ResetConsecutiveBustsForPenalty : GameRulesSettingsEvent()
    data object ResetMinPlayers : GameRulesSettingsEvent()
    data object ResetMaxPlayers : GameRulesSettingsEvent()
    data object NavigateBack : GameRulesSettingsEvent()
    data object ConfirmDiscard : GameRulesSettingsEvent()
    data object DismissDiscardDialog : GameRulesSettingsEvent()
}
