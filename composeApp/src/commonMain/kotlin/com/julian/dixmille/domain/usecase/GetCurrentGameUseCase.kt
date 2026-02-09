package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.repository.GameRepository

/**
 * Retrieves the current active game state.
 * 
 * @param repository The game repository for persistence
 */
class GetCurrentGameUseCase(
    private val repository: GameRepository
) {
    /**
     * Gets the current game.
     * 
     * @return Result containing the game, or error if no game exists
     */
    suspend operator fun invoke(): Result<Game> {
        return repository.getCurrentGame()
    }
}
