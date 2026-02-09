package com.julian.dixmille.presentation.navigation

/**
 * Navigation events emitted by ViewModels.
 * These events are collected by the Navigator to trigger navigation actions.
 */

/**
 * Navigation events from HomeScreen.
 */
sealed class HomeNavigationEvent {
    data object NavigateToGameSetup : HomeNavigationEvent()
    data object NavigateToScoreSheet : HomeNavigationEvent()
}

/**
 * Navigation events from GameSetupScreen.
 */
sealed class GameSetupNavigationEvent {
    data object NavigateToScoreSheet : GameSetupNavigationEvent()
    data object NavigateBack : GameSetupNavigationEvent()
}

/**
 * Navigation events from ScoreSheetScreen.
 */
sealed class ScoreSheetNavigationEvent {
    data class NavigateToGameEnd(
        val winnerName: String,
        val winnerScore: Int
    ) : ScoreSheetNavigationEvent()
    data object NavigateBack : ScoreSheetNavigationEvent()
}

/**
 * Navigation events from GameEndScreen.
 */
sealed class GameEndNavigationEvent {
    data object NavigateToGameSetup : GameEndNavigationEvent()
    data object NavigateToHome : GameEndNavigationEvent()
}
