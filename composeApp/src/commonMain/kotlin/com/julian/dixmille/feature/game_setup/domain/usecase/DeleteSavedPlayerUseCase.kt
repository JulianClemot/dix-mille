package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.repository.SavedPlayerRepository

class DeleteSavedPlayerUseCase(private val repository: SavedPlayerRepository) {
    suspend operator fun invoke(playerId: String): Result<Unit> = repository.deletePlayer(playerId)
}
