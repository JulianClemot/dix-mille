package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.repository.GameRulesRepository
import com.julian.dixmille.core.domain.util.UuidGenerator

/**
 * Creates a new Dix Mille game with the specified players.
 *
 * @param repository The game repository for persistence
 */
class CreateGameUseCase(
    private val repository: GameRepository,
    private val gameRulesRepository: GameRulesRepository
) {
    /**
     * Creates and saves a new game.
     *
     * Loads saved game rules (or defaults) and applies the given target score override.
     *
     * @param playerNames List of player names
     * @param targetScore Target score to win (default 10,000)
     * @return Result containing the created game, or error if validation fails
     */
    suspend operator fun invoke(
        playerNames: List<String>,
        targetScore: Int = 10_000
    ): Result<Game> = runCatching {
        val savedRules = gameRulesRepository.getRules().getOrElse { GameRules.DEFAULT }

        require(playerNames.size in savedRules.minPlayers..savedRules.maxPlayers) {
            "Game must have ${savedRules.minPlayers}-${savedRules.maxPlayers} players, got ${playerNames.size}"
        }
        require(playerNames.all { it.isNotBlank() }) {
            "All player names must be non-blank"
        }
        require(targetScore > 0) {
            "Target score must be positive, got $targetScore"
        }

        val rules = savedRules.copy(targetScore = TargetScore(targetScore))

        val players = playerNames.map { name ->
            Player(
                id = PlayerId(UuidGenerator.generate()),
                name = PlayerName(name.trim())
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
            rules = rules
        )

        // Start first player's turn
        val gameWithFirstTurn = game.updateCurrentPlayer(
            game.currentPlayer.startTurn(TurnId(UuidGenerator.generate()))
        )

        repository.saveGame(gameWithFirstTurn).getOrThrow()

        gameWithFirstTurn
    }

    // Platform-specific time implementation will be added later
    private fun currentTimeMillis(): Long = 0L
}
