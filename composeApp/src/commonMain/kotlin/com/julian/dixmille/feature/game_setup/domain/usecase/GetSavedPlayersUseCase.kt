package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.repository.SavedPlayerRepository

class GetSavedPlayersUseCase(private val repository: SavedPlayerRepository) {
    suspend operator fun invoke(): List<SavedPlayer> =
        repository.getAllPlayers().sortedBy { it.name.value.lowercase() }
}
