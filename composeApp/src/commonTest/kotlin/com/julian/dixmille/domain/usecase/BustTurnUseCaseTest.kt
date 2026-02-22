package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.util.UuidGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BustTurnUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var bustTurnUseCase: BustTurnUseCase
    private lateinit var commitTurnUseCase: CommitTurnUseCase
    private lateinit var skipTurnUseCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        bustTurnUseCase = BustTurnUseCase(repository)
        commitTurnUseCase = CommitTurnUseCase(repository)
        skipTurnUseCase = SkipTurnUseCase(repository)
    }

    @Test
    fun should_incrementBustCounter_when_playerBusts() = runTest {
        // Arrange
        val game = createGameWithTwoPlayers()
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val player1 = updatedGame.players[0]
        assertEquals(1, player1.consecutiveBusts)
        assertEquals(0, player1.totalScore)
    }

    @Test
    fun should_applyPenalty_when_thirdConsecutiveBust() = runTest {
        // Arrange
        // Player scores 500, then 200 (total 700), then busts 3 times
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500 (entry turn)
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 500, TurnOutcome.SCORED, 0)
        game = game.advanceToNextPlayer()

        // Player 2 turn (skip for simplicity)
        var player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Score 200 more (total = 700)
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(200))
        player1 = player1.commitTurn().copy(consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 200, TurnOutcome.SCORED, 500)
        game = game.advanceToNextPlayer()

        // Player 2 turn (skip)
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Now player 1 busts 3 times
        // Bust 1
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 2 (skip player 2's turn)
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 3 (skip player 2's turn) - should trigger penalty
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        // Should revert to previousScore of last SCORED turn where prev < 700
        // That's the turn where prev=500 (scored 200 to get to 700)
        assertEquals(500, player1.totalScore)
        assertEquals(0, player1.consecutiveBusts)
    }

    @Test
    fun should_triggerPenalty_when_skipBetweenBusts() = runTest {
        // Arrange - bust, bust, skip, bust should still trigger penalty
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500 first
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 500, TurnOutcome.SCORED, 0)
        game = game.advanceToNextPlayer()

        // Player 2 skip
        var player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 1
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 2
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 1 skip (skip doesn't reset counter)
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 3 - should trigger penalty
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore) // Revert to previousScore of first scored turn (0)
        assertEquals(0, player1.consecutiveBusts)
    }

    @Test
    fun should_handleCascadingPenalties() = runTest {
        // Arrange - complex scenario with multiple penalties
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0]

        // Round 1: Score 500 (total=500, prev=0)
        player1 = player1.startTurn(UuidGenerator.generate())
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 500, TurnOutcome.SCORED, 0)
        game = game.advanceToNextPlayer()
        var player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 2: Score 200 (total=700, prev=500)
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(200))
        player1 = player1.commitTurn().copy(consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 200, TurnOutcome.SCORED, 500)
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 3: Bust (busts=1, total=700)
        player1 = game.players[0].copy(consecutiveBusts = 1)
        player1 = player1.startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 0, TurnOutcome.BUST, 700)
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 4: Bust (busts=2, total=700)
        player1 = game.players[0].copy(consecutiveBusts = 2)
        player1 = player1.startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 0, TurnOutcome.BUST, 700)
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 5: Skip (busts=2, total=700)
        player1 = game.players[0].copy(consecutiveBusts = 2)
        player1 = player1.startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 0, TurnOutcome.SKIP, 700)
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 6: Bust (busts=3 -> PENALTY: revert to 500, busts=0)
        player1 = game.players[0].copy(consecutiveBusts = 2)
        player1 = player1.startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()
        player1 = game.players[0]
        assertEquals(500, player1.totalScore)
        assertEquals(0, player1.consecutiveBusts)

        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 7: Score 100 (total=600, prev=500)
        player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(100))
        player1 = player1.commitTurn().copy(consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 100, TurnOutcome.SCORED, 500)
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
        game = game.advanceToNextPlayer()

        // Round 8-10: Bust 3 times (should revert to 500 again)
        for (i in 0 until 3) {
            player1 = game.players[0].copy(consecutiveBusts = i)
            player1 = player1.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(player2)
            game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
            game = game.advanceToNextPlayer()
        }

        player1 = game.players[0]
        assertEquals(500, player1.totalScore)
        assertEquals(0, player1.consecutiveBusts)

        // Round 11-13: Bust 3 more times (should revert to 0)
        for (i in 0 until 3) {
            player1 = game.players[0].copy(consecutiveBusts = i)
            player1 = player1.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(player2)
            game = game.recordTurn(player2.id, 0, TurnOutcome.SKIP, 0)
            game = game.advanceToNextPlayer()
        }

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore)
        assertEquals(0, player1.consecutiveBusts)
    }

    @Test
    fun should_revertToZero_when_onlyOneScoringTurn() = runTest {
        // Arrange - Score 500, then bust 3 times
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 500, TurnOutcome.SCORED, 0)
        game = game.advanceToNextPlayer()

        // Bust 3 times
        for (i in 0 until 3) {
            var player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(player2)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player1 = game.players[0].copy(consecutiveBusts = i)
            player1 = player1.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore)
        assertEquals(0, player1.consecutiveBusts)
    }

    @Test
    fun should_resetCounter_when_penaltyApplied() = runTest {
        // Arrange - verify counter is reset after penalty
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(player1)

        // Score 500
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, 500, TurnOutcome.SCORED, 0)
        game = game.advanceToNextPlayer()

        // Bust 3 times
        for (i in 0 until 3) {
            var player2 = game.currentPlayer.startTurn(UuidGenerator.generate())
            game = game.updateCurrentPlayer(player2)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player1 = game.players[0].copy(consecutiveBusts = i)
            player1 = player1.startTurn(UuidGenerator.generate())
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert - counter should be 0 after penalty
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.consecutiveBusts)
    }

    private fun createGameWithTwoPlayers(): Game {
        val player1 = Player(id = "p1", name = "Alice")
        val player2 = Player(id = "p2", name = "Bob")
        return Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 1
        )
    }

    private fun createScoreEntry(points: Int): com.julian.dixmille.domain.model.ScoreEntry {
        return com.julian.dixmille.domain.model.ScoreEntry(
            id = UuidGenerator.generate(),
            points = points
        )
    }
}
