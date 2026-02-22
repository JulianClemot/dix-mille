package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.repository.GameRulesRepository
import com.julian.dixmille.domain.util.UuidGenerator

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
        val rules = savedRules.copy(targetScore = targetScore)

        require(playerNames.size in rules.minPlayers..rules.maxPlayers) {
            "Game must have ${rules.minPlayers}-${rules.maxPlayers} players, got ${playerNames.size}"
        }
        require(playerNames.all { it.isNotBlank() }) {
            "All player names must be non-blank"
        }
        require(targetScore > 0) {
            "Target score must be positive, got $targetScore"
        }

        val players = playerNames.map { name ->
            Player(
                id = UuidGenerator.generate(),
                name = name.trim()
            )
        }

        val game = Game(
            id = UuidGenerator.generate(),
            players = players,
            targetScore = targetScore,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            createdAt = currentTimeMillis(),
            rules = rules
        )
        
        // Start first player's turn
        val gameWithFirstTurn = game.updateCurrentPlayer(
            game.currentPlayer.startTurn(UuidGenerator.generate())
        )
        
        repository.saveGame(gameWithFirstTurn).getOrThrow()
        
        gameWithFirstTurn
    }
    
    // Platform-specific time implementation will be added later
    private fun currentTimeMillis(): Long = 0L
}
