package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.repository.GameRepository

class FakeGameRepository : GameRepository {
    private var game: Game? = null

    override suspend fun saveGame(game: Game): Result<Unit> {
        this.game = game
        return Result.success(Unit)
    }

    override suspend fun getCurrentGame(): Result<Game> {
        return game?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No game found"))
    }

    override suspend fun deleteGame(): Result<Unit> {
        game = null
        return Result.success(Unit)
    }

    override suspend fun hasGame(): Boolean = game != null
}
