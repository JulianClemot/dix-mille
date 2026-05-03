package com.julian.dixmille.core.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "players", indices = [Index(value = ["name"])])
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val lastPlayedAt: Long?,
)
