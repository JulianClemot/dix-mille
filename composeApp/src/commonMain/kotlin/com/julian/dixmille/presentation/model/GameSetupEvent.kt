package com.julian.dixmille.presentation.model

/**
 * Events for the Game Setup screen.
 */
sealed class GameSetupEvent {
    /**
     * Update a player name at the given index.
     */
    data class UpdatePlayerName(val index: Int, val name: String) : GameSetupEvent()
    
    /**
     * Add a new player slot.
     */
    data object AddPlayer : GameSetupEvent()
    
    /**
     * Remove a player at the given index.
     */
    data class RemovePlayer(val index: Int) : GameSetupEvent()
    
    /**
     * Update the target score.
     */
    data class UpdateTargetScore(val score: String) : GameSetupEvent()
    
    /**
     * Validate and create the game.
     */
    data object CreateGame : GameSetupEvent()
    
    /**
     * Navigate back to home.
     */
    data object NavigateBack : GameSetupEvent()
}
