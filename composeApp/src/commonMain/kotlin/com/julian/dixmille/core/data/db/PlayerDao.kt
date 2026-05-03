package com.julian.dixmille.core.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayers(): List<PlayerEntity>

    @Insert
    suspend fun insert(entity: PlayerEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM players WHERE LOWER(name) = LOWER(:name))")
    suspend fun existsByNameIgnoreCase(name: String): Boolean

    @Query("UPDATE players SET lastPlayedAt = :timestamp WHERE id = :playerId")
    suspend fun updateLastPlayedAt(playerId: String, timestamp: Long)
}
