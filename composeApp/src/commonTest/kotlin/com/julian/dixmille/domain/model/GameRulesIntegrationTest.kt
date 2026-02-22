package com.julian.dixmille.domain.model

import com.julian.dixmille.domain.usecase.BustTurnUseCase
import com.julian.dixmille.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.domain.usecase.FakeGameRepository
import com.julian.dixmille.domain.usecase.SkipTurnUseCase
import com.julian.dixmille.domain.util.UuidGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRulesIntegrationTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase
    private lateinit var bustTurnUseCase: BustTurnUseCase
    private lateinit var skipTurnUseCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        commitTurnUseCase = CommitTurnUseCase(repository)
        bustTurnUseCase = BustTurnUseCase(repository)
        skipTurnUseCase = SkipTurnUseCase(repository)
    }

    @Test
    fun should_notApplyBustPenalty_when_bustPenaltyDisabled() = runTest {
        // Arrange: Game with bust penalty disabled
        val rules = GameRules(enableBustPenalty = false)
        var game = createGame(rules)

        // Player A scores 500
        var playerA = game.players[0].startTurn(UuidGenerator.generate())
        playerA = playerA.addScoreEntry(createScoreEntry(500))
        game = game.updateCurrentPlayer(playerA)
        repository.saveGame(game)
        commitTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 3 times (with player B skipping between)
        for (i in 0 until 3) {
            // Player B skips
            var playerB = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(playerB)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            // Player A busts
            playerA = game.players[0].copy(consecutiveBusts = i)
            playerA = playerA.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(playerA)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert: Score should NOT be reverted (penalty disabled)
        val finalGame = repository.getCurrentGame().getOrThrow()
        assertEquals(500, finalGame.players[0].totalScore)
        // Bust counter should still increment (but no penalty applied)
        assertEquals(3, finalGame.players[0].consecutiveBusts)
    }

    @Test
    fun should_useCustomEntryMinimum_when_configured() = runTest {
        // Arrange: Game with 300-point entry minimum
        val rules = GameRules(entryMinimumScore = 300)
        var game = createGame(rules)

        // Player A scores 300 (should enter the game with custom minimum)
        var playerA = game.players[0].startTurn(UuidGenerator.generate())
        playerA = playerA.addScoreEntry(createScoreEntry(300))
        game = game.updateCurrentPlayer(playerA)
        repository.saveGame(game)
        commitTurnUseCase()

        // Assert: Player should have entered and scored
        val finalGame = repository.getCurrentGame().getOrThrow()
        assertEquals(300, finalGame.players[0].totalScore)
        assertEquals(true, finalGame.players[0].hasEnteredGame)
    }

    @Test
    fun should_endImmediately_when_finalRoundDisabledAndTargetReached() = runTest {
        // Arrange: Game with 1000 target and final round disabled
        val rules = GameRules(targetScore = 1000, enableFinalRound = false)
        var game = createGame(rules)

        // Player A scores 1000
        var playerA = game.players[0].startTurn(UuidGenerator.generate())
        playerA = playerA.addScoreEntry(createScoreEntry(1000))
        game = game.updateCurrentPlayer(playerA)
        repository.saveGame(game)
        commitTurnUseCase()

        // Assert: Game should be ENDED immediately (no final round)
        val finalGame = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, finalGame.gamePhase)
        assertEquals(null, finalGame.triggeringPlayerId) // No triggering player since no final round
    }

    @Test
    fun should_applyBustPenalty_when_customThresholdReached() = runTest {
        // Arrange: Game with 2-bust penalty
        val rules = GameRules(consecutiveBustsForPenalty = 2)
        var game = createGame(rules)

        // Player A scores 500
        var playerA = game.players[0].startTurn(UuidGenerator.generate())
        playerA = playerA.addScoreEntry(createScoreEntry(500))
        game = game.updateCurrentPlayer(playerA)
        repository.saveGame(game)
        commitTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 2 times (with player B skipping between)
        for (i in 0 until 2) {
            // Player B skips
            var playerB = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(playerB)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            // Player A busts
            playerA = game.players[0].copy(consecutiveBusts = i)
            playerA = playerA.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(playerA)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert: 2-bust penalty should have triggered (reverts to 0)
        val finalGame = repository.getCurrentGame().getOrThrow()
        assertEquals(0, finalGame.players[0].totalScore)
        assertEquals(0, finalGame.players[0].consecutiveBusts)
    }

    private fun createGame(rules: GameRules): Game {
        val player1 = Player(id = "p1", name = "Alice")
        val player2 = Player(id = "p2", name = "Bob")
        return Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = rules.targetScore,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            rules = rules
        )
    }

    private fun createScoreEntry(points: Int): ScoreEntry {
        return ScoreEntry(
            id = UuidGenerator.generate(),
            points = points
        )
    }
}
