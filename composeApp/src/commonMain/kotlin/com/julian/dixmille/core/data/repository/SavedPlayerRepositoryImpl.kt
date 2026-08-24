package com.julian.dixmille.core.data.repository

import com.julian.dixmille.core.data.db.PlayerDao
import com.julian.dixmille.core.data.mapper.toDomain
import com.julian.dixmille.core.data.mapper.toEntity
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.repository.SavedPlayerRepository

class SavedPlayerRepositoryImpl(private val playerDao: PlayerDao) : SavedPlayerRepository {

    override suspend fun getAllPlayers(): List<SavedPlayer> =
        playerDao.getAllPlayers().map { it.toDomain() }

    override suspend fun addPlayer(player: SavedPlayer): Result<SavedPlayer> {
        if (playerDao.existsByNameIgnoreCase(player.name.value)) {
            return Result.failure(
                IllegalArgumentException("Player with name '${player.name.value}' already exists"),
            )
        }
        playerDao.insert(player.toEntity())
        return Result.success(player)
    }

    override suspend fun updateLastPlayedAt(playerId: String, timestamp: Long) {
        playerDao.updateLastPlayedAt(playerId, timestamp)
    }

    override suspend fun playerExistsByNameIgnoreCase(name: String): Boolean =
        playerDao.existsByNameIgnoreCase(name)

    override suspend fun deletePlayer(playerId: String): Result<Unit> = runCatching {
        playerDao.deletePlayer(playerId)
    }
}
