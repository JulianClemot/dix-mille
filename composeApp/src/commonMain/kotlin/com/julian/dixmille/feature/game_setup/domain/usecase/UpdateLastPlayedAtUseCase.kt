package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.repository.SavedPlayerRepository

class UpdateLastPlayedAtUseCase(
    private val repository: SavedPlayerRepository,
    private val clock: () -> Long = { 0L },
) {
    suspend operator fun invoke(playerIds: List<String>) {
        val timestamp = clock()
        playerIds.forEach { repository.updateLastPlayedAt(it, timestamp) }
    }
}
