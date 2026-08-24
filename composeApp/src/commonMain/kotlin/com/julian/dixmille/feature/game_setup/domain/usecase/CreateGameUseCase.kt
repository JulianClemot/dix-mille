package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.repository.GameRulesRepository
import com.julian.dixmille.core.domain.repository.SavedPlayerRepository
import com.julian.dixmille.core.domain.util.UuidGenerator

private object NoOpSavedPlayerRepository : SavedPlayerRepository {
    override suspend fun getAllPlayers(): List<SavedPlayer> = emptyList()
    override suspend fun addPlayer(player: SavedPlayer): Result<SavedPlayer> = Result.success(player)
    override suspend fun updateLastPlayedAt(playerId: String, timestamp: Long) = Unit
    override suspend fun playerExistsByNameIgnoreCase(name: String): Boolean = false
    override suspend fun deletePlayer(playerId: String): Result<Unit> = Result.success(Unit)
}

class CreateGameUseCase(
    private val repository: GameRepository,
    private val gameRulesRepository: GameRulesRepository,
    private val updateLastPlayedAtUseCase: UpdateLastPlayedAtUseCase = UpdateLastPlayedAtUseCase(NoOpSavedPlayerRepository),
) {
    suspend operator fun invoke(
        selectedPlayers: List<SavedPlayer>,
        targetScore: Int = 10_000,
    ): Result<Game> = runCatching {
        val savedRules = gameRulesRepository.getRules().getOrElse { GameRules.DEFAULT }

        require(selectedPlayers.size in savedRules.minPlayers..savedRules.maxPlayers) {
            "Game must have ${savedRules.minPlayers}-${savedRules.maxPlayers} players, got ${selectedPlayers.size}"
        }
        require(selectedPlayers.all { it.name.value.isNotBlank() }) {
            "All player names must be non-blank"
        }
        require(targetScore > 0) {
            "Target score must be positive, got $targetScore"
        }

        val rules = savedRules.copy(targetScore = TargetScore(targetScore))

        val players = selectedPlayers.map { savedPlayer ->
            Player(
                id = PlayerId(UuidGenerator.generate()),
                name = savedPlayer.name,
            )
        }

        val game = Game(
            id = GameId(UuidGenerator.generate()),
            players = players,
            targetScore = rules.targetScore,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            createdAt = currentTimeMillis(),
            rules = rules,
        )

        val gameWithFirstTurn = game.updateCurrentPlayer(
            game.currentPlayer.startTurn(TurnId(UuidGenerator.generate()))
        )

        repository.saveGame(gameWithFirstTurn).getOrThrow()

        updateLastPlayedAtUseCase(selectedPlayers.map { it.id.value })

        gameWithFirstTurn
    }

    private fun currentTimeMillis(): Long = 0L
}
