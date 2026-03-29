package com.julian.dixmille.core.data.repository

import com.julian.dixmille.core.data.mapper.toDomain
import com.julian.dixmille.core.data.mapper.toDto
import com.julian.dixmille.core.data.model.GameDto
import com.julian.dixmille.core.data.source.LocalStorage
import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.repository.GameRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of GameRepository using local storage.
 *
 * Persists game state as JSON in platform-specific storage.
 * Uses [GameDto] as the serialization boundary so that domain models
 * are decoupled from kotlinx.serialization.
 */
class GameRepositoryImpl(
    private val localStorage: LocalStorage
) : GameRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveGame(game: Game): Result<Unit> = runCatching {
        val dto = game.toDto()
        val gameJson = json.encodeToString(GameDto.serializer(), dto)
        localStorage.saveString(GAME_KEY, gameJson)
    }

    override suspend fun getCurrentGame(): Result<Game> = runCatching {
        val gameJson = localStorage.getString(GAME_KEY)
            ?: throw NoSuchElementException("No game found")

        json.decodeFromString(GameDto.serializer(), gameJson).toDomain()
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
