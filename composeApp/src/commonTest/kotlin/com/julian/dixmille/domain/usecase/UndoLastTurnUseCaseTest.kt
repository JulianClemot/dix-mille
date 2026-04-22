package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastTurnUseCase
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
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
    fun `Should restore bust counter when undoing bust`() = runTest {
        // Arrange - Player has 1 bust, undo it -> counter back to 0
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true, totalScore = Score(500), consecutiveBusts = BustCount(1))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score.ZERO,
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore.value)
        assertEquals(0, updatedPlayer.consecutiveBusts.value) // Should be back to 0
    }

    @Test
    fun `Should restore score when undoing penalty bust`() = runTest {
        // Arrange - Player had score 700, 3rd bust triggered penalty reducing to 500
        // Undo the 3rd bust -> score back to 700, counter back to 2
        val player1 = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            hasEnteredGame = true,
            totalScore = Score(500), // After penalty
            consecutiveBusts = BustCount.NONE  // Reset after penalty
        )
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score(200),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 3,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(700)
                ),
                TurnRecord(
                    roundNumber = 4,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(700)
                ),
                TurnRecord(
                    roundNumber = 5,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(700)  // This was the 3rd bust that triggered penalty
                )
            ),
            roundNumber = 5
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(700, updatedPlayer.totalScore.value) // Score restored to before 3rd bust
        assertEquals(2, updatedPlayer.consecutiveBusts.value) // Counter back to 2
    }

    @Test
    fun `Should re-derive bust counter when undoing scored turn`() = runTest {
        // Arrange - Player had 2 busts then scored (resetting counter to 0)
        // Undo the scored turn -> counter back to 2
        val player1 = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            hasEnteredGame = true,
            totalScore = Score(700),
            consecutiveBusts = BustCount.NONE  // Reset after scoring
        )
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score.ZERO,
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 3,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 4,
                    playerId = PlayerId("p1"),
                    points = Score(200),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(500)
                )
            ),
            roundNumber = 4
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore.value) // Score back to before scored turn
        assertEquals(2, updatedPlayer.consecutiveBusts.value) // Counter re-derived: 2 consecutive busts
    }

    @Test
    fun `Should handle skip in bust sequence when undoing`() = runTest {
        // Arrange - Player: scored, bust, bust, skip -> counter stays at 2
        // Then scored -> counter resets to 0
        // Undo the scored turn -> counter should be 2 (skip doesn't break the sequence)
        val player1 = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            hasEnteredGame = true,
            totalScore = Score(700),
            consecutiveBusts = BustCount.NONE
        )
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score.ZERO,
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 3,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 4,
                    playerId = PlayerId("p1"),
                    points = Score(0),
                    outcome = TurnOutcome.SKIP,
                    previousScore = Score(500)
                ),
                TurnRecord(
                    roundNumber = 5,
                    playerId = PlayerId("p1"),
                    points = Score(200),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(500)
                )
            ),
            roundNumber = 5
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore.value)
        assertEquals(2, updatedPlayer.consecutiveBusts.value) // 2 busts before the skip
    }

    @Test
    fun `Should handle zero bust counter when undoing first bust`() = runTest {
        // Arrange - Player scored, then busted once
        // Undo the bust -> counter should be 0
        val player1 = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            hasEnteredGame = true,
            totalScore = Score(500),
            consecutiveBusts = BustCount(1)
        )
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score.ZERO,
                    outcome = TurnOutcome.BUST,
                    previousScore = Score(500)
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(500, updatedPlayer.totalScore.value)
        assertEquals(0, updatedPlayer.consecutiveBusts.value)
    }

    @Test
    fun `Should revert score and restore turn when undoing scored turn`() = runTest {
        // Arrange - Alice committed 300 points (500->800), now Bob's turn
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true, totalScore = Score(800))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                ),
                TurnRecord(
                    roundNumber = 2,
                    playerId = PlayerId("p1"),
                    points = Score(300),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(500)
                )
            ),
            roundNumber = 2
        )
        val currentPlayer = game.players[1].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedAlice = updatedGame.players[0]
        assertEquals(500, updatedAlice.totalScore.value)
        assertEquals(0, updatedGame.currentPlayerIndex)
        assertEquals(1, updatedGame.turnHistory.size) // SCORED record for 300 pts removed
    }

    @Test
    fun `Should revert hasEnteredGame when undoing entry turn`() = runTest {
        // Arrange - Alice entered game with 600 pts (0->600, hasEnteredGame=true), now Bob's turn
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true, totalScore = Score(600))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(600),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(0)
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[1].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedAlice = updatedGame.players[0]
        assertEquals(0, updatedAlice.totalScore.value)
        assertEquals(false, updatedAlice.hasEnteredGame)
    }

    @Test
    fun `Should revert hasPlayedFinalRound when undoing final round turn`() = runTest {
        // Arrange - FINAL_ROUND, Alice is triggering player
        // Bob committed his final turn (hasPlayedFinalRound=true, 300->500), now Alice's turn (index=0)
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true, totalScore = Score(10_000))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), hasEnteredGame = true, totalScore = Score(500), hasPlayedFinalRound = true)
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(10_000),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(0)
                ),
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p2"),
                    points = Score(200),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(300)
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
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
    fun `Should revert game phase to final round when undoing last turn of game`() = runTest {
        // Arrange - Game just ENDED after Bob's final round turn
        val player1 = Player(id = PlayerId("p1"), name = PlayerName("Alice"), hasEnteredGame = true, totalScore = Score(10_000))
        val player2 = Player(id = PlayerId("p2"), name = PlayerName("Bob"), hasEnteredGame = true, totalScore = Score(500), hasPlayedFinalRound = true)
        var game = Game(
            id = GameId("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(10_000),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(0)
                ),
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p2"),
                    points = Score(200),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(300)
                )
            ),
            roundNumber = 1
        )
        val currentPlayer = game.players[0].startTurn(TurnId(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(currentPlayer)
        repository.saveGame(game)

        // Act
        undoLastTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.FINAL_ROUND, updatedGame.gamePhase)
    }
}
