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

class UndoLastTurnUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var undoLastTurnUseCase: UndoLastTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        undoLastTurnUseCase = UndoLastTurnUseCase(repository)
    }

    @Test
    fun should_restoreBustCounter_when_undoingBust() = runTest {
        // Arrange - Player has 1 bust, undo it -> counter back to 0
        val player1 = Player(id = "p1", name = "Alice", hasEnteredGame = true, totalScore = 500, consecutiveBusts = 1)
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore)
        assertEquals(0, updatedPlayer.consecutiveBusts) // Should be back to 0
    }

    @Test
    fun should_restoreScore_when_undoingPenaltyBust() = runTest {
        // Arrange - Player had score 700, 3rd bust triggered penalty reducing to 500
        // Undo the 3rd bust -> score back to 700, counter back to 2
        val player1 = Player(
            id = "p1",
            name = "Alice",
            hasEnteredGame = true,
            totalScore = 500, // After penalty
            consecutiveBusts = 0  // Reset after penalty
        )
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 200,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 3,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 700
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 4,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 700
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 5,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 700  // This was the 3rd bust that triggered penalty
                )
            ),
            roundNumber = 5
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(700, updatedPlayer.totalScore) // Score restored to before 3rd bust
        assertEquals(2, updatedPlayer.consecutiveBusts) // Counter back to 2
    }

    @Test
    fun should_rederiveBustCounter_when_undoingScoredTurn() = runTest {
        // Arrange - Player had 2 busts then scored (resetting counter to 0)
        // Undo the scored turn -> counter back to 2
        val player1 = Player(
            id = "p1",
            name = "Alice",
            hasEnteredGame = true,
            totalScore = 700,
            consecutiveBusts = 0  // Reset after scoring
        )
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 3,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 4,
                    playerId = "p1",
                    points = 200,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 500
                )
            ),
            roundNumber = 4
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore) // Score back to before scored turn
        assertEquals(2, updatedPlayer.consecutiveBusts) // Counter re-derived: 2 consecutive busts
    }

    @Test
    fun should_handleSkipInBustSequence_when_undoing() = runTest {
        // Arrange - Player: scored, bust, bust, skip -> counter stays at 2
        // Then scored -> counter resets to 0
        // Undo the scored turn -> counter should be 2 (skip doesn't break the sequence)
        val player1 = Player(
            id = "p1",
            name = "Alice",
            hasEnteredGame = true,
            totalScore = 700,
            consecutiveBusts = 0
        )
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 3,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 4,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.SKIP,
                    previousScore = 500
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 5,
                    playerId = "p1",
                    points = 200,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 500
                )
            ),
            roundNumber = 5
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore)
        assertEquals(2, updatedPlayer.consecutiveBusts) // 2 busts before the skip
    }

    @Test
    fun should_handleZeroBustCounter_when_undoingFirstBust() = runTest {
        // Arrange - Player scored, then busted once
        // Undo the bust -> counter should be 0
        val player1 = Player(
            id = "p1",
            name = "Alice",
            hasEnteredGame = true,
            totalScore = 500,
            consecutiveBusts = 1
        )
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 0,
                    outcome = TurnOutcome.BUST,
                    previousScore = 500
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore)
        assertEquals(0, updatedPlayer.consecutiveBusts)
    }

    @Test
    fun should_revertScoreAndRestoreTurn_when_undoingScoredTurn() = runTest {
        // Arrange - Alice committed 300 points (500->800), now Bob's turn
        val player1 = Player(id = "p1", name = "Alice", hasEnteredGame = true, totalScore = 800)
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 500,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 2,
                    playerId = "p1",
                    points = 300,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 500
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[1].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedAlice = updatedGame.players[0]
        assertEquals(500, updatedAlice.totalScore)
        assertEquals(0, updatedGame.currentPlayerIndex)
        assertEquals(1, updatedGame.turnHistory.size) // SCORED record for 300 pts removed
    }

    @Test
    fun should_revertHasEnteredGame_when_undoingEntryTurn() = runTest {
        // Arrange - Alice entered game with 600 pts (0->600, hasEnteredGame=true), now Bob's turn
        val player1 = Player(id = "p1", name = "Alice", hasEnteredGame = true, totalScore = 600)
        val player2 = Player(id = "p2", name = "Bob")
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 600,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[1].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedAlice = updatedGame.players[0]
        assertEquals(0, updatedAlice.totalScore)
        assertEquals(false, updatedAlice.hasEnteredGame)
    }

    @Test
    fun should_revertHasPlayedFinalRound_when_undoingFinalRoundTurn() = runTest {
        // Arrange - FINAL_ROUND, Alice is triggering player
        // Bob committed his final turn (hasPlayedFinalRound=true, 300->500), now Alice's turn (index=0)
        val player1 = Player(id = "p1", name = "Alice", hasEnteredGame = true, totalScore = 10_000)
        val player2 = Player(id = "p2", name = "Bob", hasEnteredGame = true, totalScore = 500, hasPlayedFinalRound = true)
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = "p1",
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 10_000,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p2",
                    points = 200,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 300
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedBob = updatedGame.players[1]
        assertEquals(false, updatedBob.hasPlayedFinalRound)
        assertEquals(1, updatedGame.currentPlayerIndex) // Bob's turn again
    }

    @Test
    fun should_revertGamePhaseToFinalRound_when_undoingLastTurnOfGame() = runTest {
        // Arrange - Game just ENDED after Bob's final round turn
        val player1 = Player(id = "p1", name = "Alice", hasEnteredGame = true, totalScore = 10_000)
        val player2 = Player(id = "p2", name = "Bob", hasEnteredGame = true, totalScore = 500, hasPlayedFinalRound = true)
        var game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            triggeringPlayerId = "p1",
            createdAt = 0L,
            turnHistory = listOf(
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p1",
                    points = 10_000,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 0
                ),
                com.julian.dixmille.domain.model.TurnRecord(
                    roundNumber = 1,
                    playerId = "p2",
                    points = 200,
                    outcome = TurnOutcome.SCORED,
                    previousScore = 300
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[0].startTurn(UuidGenerator.generate())
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.FINAL_ROUND, updatedGame.gamePhase)
    }
}
