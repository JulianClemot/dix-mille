package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.SavedPlayerRepository
import com.julian.dixmille.core.domain.util.UuidGenerator

class AddSavedPlayerUseCase(
    private val repository: SavedPlayerRepository,
    private val generateId: () -> String = { UuidGenerator.generate() },
    private val clock: () -> Long = { 0L },
) {
    suspend operator fun invoke(name: String): Result<SavedPlayer> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Player name cannot be blank" }
        val player = SavedPlayer(
            id = PlayerId(generateId()),
            name = PlayerName(trimmed),
            createdAt = clock(),
            lastPlayedAt = null,
        )
        repository.addPlayer(player).getOrThrow()
    }
}
