package com.julian.dixmille.data.repository

import com.julian.dixmille.data.source.LocalStorage
import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.repository.GameRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of GameRepository using local storage.
 * 
 * Persists game state as JSON in platform-specific storage.
 */
class GameRepositoryImpl(
    private val localStorage: LocalStorage
) : GameRepository {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun saveGame(game: Game): Result<Unit> = runCatching {
        val gameJson = json.encodeToString(game)
        localStorage.saveString(GAME_KEY, gameJson)
    }
    
    override suspend fun getCurrentGame(): Result<Game> = runCatching {
        val gameJson = localStorage.getString(GAME_KEY)
            ?: throw NoSuchElementException("No game found")
        
        json.decodeFromString<Game>(gameJson)
    }
    
    override suspend fun deleteGame(): Result<Unit> = runCatching {
        localStorage.remove(GAME_KEY)
    }
    
    override suspend fun hasGame(): Boolean {
        return localStorage.getString(GAME_KEY) != null
    }
    
    companion object {
        private const val GAME_KEY = "current_game"
    }
}
