package com.julian.dixmille.domain.repository

import com.julian.dixmille.domain.model.Game

/**
 * Repository interface for game persistence.
 * 
 * This interface defines the contract for storing and retrieving game state.
 * Implementations handle platform-specific storage (SharedPreferences, UserDefaults, etc.).
 */
interface GameRepository {
    
    /**
     * Saves the current game state.
     * 
     * @param game The game to save
     * @return Result containing Unit on success, or exception on failure
     */
    suspend fun saveGame(game: Game): Result<Unit>
    
    /**
     * Retrieves the current active game.
     * 
     * @return Result containing the game if found, or exception if not found or error
     */
    suspend fun getCurrentGame(): Result<Game>
    
    /**
     * Deletes the current game.
     * 
     * @return Result containing Unit on success, or exception on failure
     */
    suspend fun deleteGame(): Result<Unit>
    
    /**
     * Checks if a game currently exists.
     * 
     * @return True if a game exists, false otherwise
     */
    suspend fun hasGame(): Boolean
}
