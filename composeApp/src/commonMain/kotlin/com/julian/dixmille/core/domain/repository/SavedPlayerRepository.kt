package com.julian.dixmille.core.domain.repository

import com.julian.dixmille.core.domain.model.SavedPlayer

interface SavedPlayerRepository {
    suspend fun getAllPlayers(): List<SavedPlayer>
    suspend fun addPlayer(player: SavedPlayer): Result<SavedPlayer>
    suspend fun updateLastPlayedAt(playerId: String, timestamp: Long)
    suspend fun playerExistsByNameIgnoreCase(name: String): Boolean
}
