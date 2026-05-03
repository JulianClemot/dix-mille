package com.julian.dixmille.core.domain.repository

import com.julian.dixmille.core.domain.model.SavedPlayer

class FakeSavedPlayerRepository : SavedPlayerRepository {
    val players = mutableListOf<SavedPlayer>()

    override suspend fun getAllPlayers(): List<SavedPlayer> = players.toList()

    override suspend fun addPlayer(player: SavedPlayer): Result<SavedPlayer> {
        val duplicate = players.any { it.name.value.equals(player.name.value, ignoreCase = true) }
        if (duplicate) return Result.failure(IllegalArgumentException("Duplicate name"))
        players += player
        return Result.success(player)
    }

    override suspend fun updateLastPlayedAt(playerId: String, timestamp: Long) {
        val index = players.indexOfFirst { it.id.value == playerId }
        if (index >= 0) players[index] = players[index].copy(lastPlayedAt = timestamp)
    }

    override suspend fun playerExistsByNameIgnoreCase(name: String): Boolean =
        players.any { it.name.value.equals(name, ignoreCase = true) }
}
