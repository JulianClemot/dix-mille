package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName

data class SavedPlayer(
    val id: PlayerId,
    val name: PlayerName,
    val createdAt: Long,
    val lastPlayedAt: Long?,
)
