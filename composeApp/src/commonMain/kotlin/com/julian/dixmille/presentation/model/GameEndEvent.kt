package com.julian.dixmille.presentation.model

/**
 * Events for the Game End screen.
 */
sealed class GameEndEvent {
    /**
     * Start a new game (clear current game and navigate to setup).
     */
    data object StartNewGame : GameEndEvent()
    
    /**
     * Return to home screen (clear current game).
     */
    data object ReturnHome : GameEndEvent()
}
