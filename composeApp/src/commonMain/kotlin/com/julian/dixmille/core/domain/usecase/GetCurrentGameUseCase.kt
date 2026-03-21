package com.julian.dixmille.core.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.repository.GameRepository

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
