package com.julian.dixmille.presentation.model

/**
 * Events for the Home screen.
 */
sealed class HomeEvent {
    /**
     * Navigate to game setup screen.
     */
    data object NavigateToNewGame : HomeEvent()
    
    /**
     * Navigate to resume existing game.
     */
    data object NavigateToResumeGame : HomeEvent()
}
