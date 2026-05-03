package com.julian.dixmille.core.data.mapper

import com.julian.dixmille.core.data.db.PlayerEntity
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName

fun PlayerEntity.toDomain(): SavedPlayer = SavedPlayer(
    id = PlayerId(id),
    name = PlayerName(name),
    createdAt = createdAt,
    lastPlayedAt = lastPlayedAt,
)

fun SavedPlayer.toEntity(): PlayerEntity = PlayerEntity(
    id = id.value,
    name = name.value,
    createdAt = createdAt,
    lastPlayedAt = lastPlayedAt,
)
