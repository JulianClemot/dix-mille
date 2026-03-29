package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
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
    fun `Should increment bust counter when player busts`() = runTest {
        // Arrange
        val game = createGameWithTwoPlayers()
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val player1 = updatedGame.players[0]
        assertEquals(1, player1.consecutiveBusts.value)
        assertEquals(0, player1.totalScore.value)
    }

    @Test
    fun `Should apply penalty when third consecutive bust`() = runTest {
        // Arrange
        // Player scores 500, then 200 (total 700), then busts 3 times
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500 (entry turn)
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))
        game = game.advanceToNextPlayer()

        // Player 2 turn (skip for simplicity)
        var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Score 200 more (total = 700)
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(200))
        player1 = player1.commitTurn().copy(consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(200), TurnOutcome.SCORED, Score.of(500))
        game = game.advanceToNextPlayer()

        // Player 2 turn (skip)
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Now player 1 busts 3 times
        // Bust 1
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 2 (skip player 2's turn)
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 3 (skip player 2's turn) - should trigger penalty
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
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
        assertEquals(500, player1.totalScore.value)
        assertEquals(0, player1.consecutiveBusts.value)
    }

    @Test
    fun `Should trigger penalty when skip occurs between busts`() = runTest {
        // Arrange - bust, bust, skip, bust should still trigger penalty
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500 first
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))
        game = game.advanceToNextPlayer()

        // Player 2 skip
        var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 1
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 2
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 1 skip (skip doesn't reset counter)
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 2 skip
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Bust 3 - should trigger penalty
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore.value) // Revert to previousScore of first scored turn (0)
        assertEquals(0, player1.consecutiveBusts.value)
    }

    @Test
    fun `Should handle cascading penalties`() = runTest {
        // Arrange - complex scenario with multiple penalties
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0]

        // Round 1: Score 500 (total=500, prev=0)
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))
        game = game.advanceToNextPlayer()
        var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 2: Score 200 (total=700, prev=500)
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(200))
        player1 = player1.commitTurn().copy(consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(200), TurnOutcome.SCORED, Score.of(500))
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 3: Bust (busts=1, total=700)
        player1 = game.players[0].copy(consecutiveBusts = BustCount.of(1))
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(0), TurnOutcome.BUST, Score.of(700))
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 4: Bust (busts=2, total=700)
        player1 = game.players[0].copy(consecutiveBusts = BustCount.of(2))
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(0), TurnOutcome.BUST, Score.of(700))
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 5: Skip (busts=2, total=700)
        player1 = game.players[0].copy(consecutiveBusts = BustCount.of(2))
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(0), TurnOutcome.SKIP, Score.of(700))
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 6: Bust (busts=3 -> PENALTY: revert to 500, busts=0)
        player1 = game.players[0].copy(consecutiveBusts = BustCount.of(2))
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        bustTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()
        player1 = game.players[0]
        assertEquals(500, player1.totalScore.value)
        assertEquals(0, player1.consecutiveBusts.value)

        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 7: Score 100 (total=600, prev=500)
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        player1 = player1.addScoreEntry(createScoreEntry(100))
        player1 = player1.commitTurn().copy(consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(100), TurnOutcome.SCORED, Score.of(500))
        game = game.advanceToNextPlayer()
        player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
        game = game.advanceToNextPlayer()

        // Round 8-10: Bust 3 times (should revert to 500 again)
        for (i in 0 until 3) {
            player1 = game.players[0].copy(consecutiveBusts = BustCount.of(i))
            player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(player2)
            game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
            game = game.advanceToNextPlayer()
        }

        player1 = game.players[0]
        assertEquals(500, player1.totalScore.value)
        assertEquals(0, player1.consecutiveBusts.value)

        // Round 11-13: Bust 3 more times (should revert to 0)
        for (i in 0 until 3) {
            player1 = game.players[0].copy(consecutiveBusts = BustCount.of(i))
            player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(player2)
            game = game.recordTurn(player2.id, Score.of(0), TurnOutcome.SKIP, Score.of(0))
            game = game.advanceToNextPlayer()
        }

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore.value)
        assertEquals(0, player1.consecutiveBusts.value)
    }

    @Test
    fun `Should revert to zero when only one scoring turn`() = runTest {
        // Arrange - Score 500, then bust 3 times
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Score 500
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))
        game = game.advanceToNextPlayer()

        // Bust 3 times
        for (i in 0 until 3) {
            var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(player2)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player1 = game.players[0].copy(consecutiveBusts = BustCount.of(i))
            player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.totalScore.value)
        assertEquals(0, player1.consecutiveBusts.value)
    }

    @Test
    fun `Should reset counter when penalty applied`() = runTest {
        // Arrange - verify counter is reset after penalty
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)

        // Score 500
        player1 = player1.addScoreEntry(createScoreEntry(500))
        player1 = player1.commitTurn().copy(hasEnteredGame = true, consecutiveBusts = BustCount.NONE)
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))
        game = game.advanceToNextPlayer()

        // Bust 3 times
        for (i in 0 until 3) {
            var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.updateCurrentPlayer(player2)
            repository.saveGame(game)
            skipTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()

            player1 = game.players[0].copy(consecutiveBusts = BustCount.of(i))
            player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
            game = game.copy(currentPlayerIndex = 0)
            game = game.updateCurrentPlayer(player1)
            repository.saveGame(game)
            bustTurnUseCase()
            game = repository.getCurrentGame().getOrThrow()
        }

        // Assert - counter should be 0 after penalty
        val finalGame = repository.getCurrentGame().getOrThrow()
        player1 = finalGame.players[0]
        assertEquals(0, player1.consecutiveBusts.value)
    }

    // ── New BDD-aligned tests ─────────────────────────────────────────────────

    @Test
    fun `Should discard turn points when player busts`() = runTest {
        // Arrange
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].copy(
            totalScore = Score.of(500),
            hasEnteredGame = true,
            consecutiveBusts = BustCount.NONE
        )
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        player1 = player1.addScoreEntry(createScoreEntry(300))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(500, updatedGame.players[0].totalScore.value)
    }

    @Test
    fun `Should record bust turn in history when player busts`() = runTest {
        // Arrange
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].copy(
            totalScore = Score.of(500),
            hasEnteredGame = true
        )
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(1, updatedGame.turnHistory.size)
        val record = updatedGame.turnHistory[0]
        assertEquals(com.julian.dixmille.core.domain.model.TurnOutcome.BUST, record.outcome)
        assertEquals(0, record.points.value)
        assertEquals(500, record.previousScore.value)
    }

    @Test
    fun `Should advance to next player when player busts`() = runTest {
        // Arrange
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].copy(hasEnteredGame = true, totalScore = Score.of(500))
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(1, updatedGame.currentPlayerIndex)
        kotlin.test.assertNotNull(updatedGame.players[1].currentTurn)
    }

    @Test
    fun `Should increment bust counter when unentered player busts`() = runTest {
        // Arrange
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0] // hasEnteredGame=false, totalScore=Score.of(0)
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        assertEquals(1, updatedGame.players[0].consecutiveBusts.value)
        assertEquals(0, updatedGame.players[0].totalScore.value)
    }

    @Test
    fun `Should not apply penalty when bust penalty is disabled`() = runTest {
        // Arrange - bust penalty disabled, Alice has 2 consecutive busts, busts a 3rd time
        val rules = GameRules.DEFAULT.copy(enableBustPenalty = false)
        var game = createGameWithTwoPlayers().copy(rules = rules)
        var player1 = game.players[0].copy(
            totalScore = Score.of(700),
            hasEnteredGame = true,
            consecutiveBusts = BustCount.of(2)
        )
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(700, updatedPlayer.totalScore.value)
        assertEquals(3, updatedPlayer.consecutiveBusts.value)
    }

    @Test
    fun `Should apply penalty when custom threshold reached`() = runTest {
        // Arrange - threshold is 2 busts; Alice has 1 consecutive bust and a SCORED history entry
        val rules = GameRules.DEFAULT.copy(consecutiveBustsForPenalty = 2)
        var game = createGameWithTwoPlayers().copy(rules = rules)

        // Set up Alice with score 700, 1 consecutive bust, and a SCORED turn history entry
        // (previousScore = 0, i.e. she scored 700 from 0)
        var player1 = game.players[0].copy(
            totalScore = Score.of(700),
            hasEnteredGame = true,
            consecutiveBusts = BustCount.of(1)
        )
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(700), TurnOutcome.SCORED, Score.of(0))
        repository.saveGame(game)

        // Act – 2nd bust, should trigger penalty
        bustTurnUseCase()

        // Assert – score reverts to previousScore of last SCORED turn (0)
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(0, updatedPlayer.totalScore.value)
        assertEquals(0, updatedPlayer.consecutiveBusts.value)
    }

    @Test
    fun `Should reset bust counter when player scores between busts`() = runTest {
        // Arrange - Alice has 2 busts, then scores 200 via CommitTurnUseCase (counter resets),
        // then busts once; consecutiveBusts should be 1 and no penalty
        var game = createGameWithTwoPlayers()
        var player1 = game.players[0].copy(
            totalScore = Score.of(500),
            hasEnteredGame = true,
            consecutiveBusts = BustCount.of(2)
        )
        player1 = player1.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player1)
        game = game.recordTurn(player1.id, Score.of(500), TurnOutcome.SCORED, Score.of(0))

        // Score 200 via CommitTurnUseCase (resets bust counter to 0)
        player1 = game.players[0].addScoreEntry(createScoreEntry(200))
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)
        commitTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Now it's player 2's turn — advance back to player 1 for the bust
        var player2 = game.currentPlayer.startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.updateCurrentPlayer(player2)
        repository.saveGame(game)
        skipTurnUseCase()
        game = repository.getCurrentGame().getOrThrow()

        // Player 1 busts once
        player1 = game.players[0].startTurn(TurnId.of(UuidGenerator.generate()))
        game = game.copy(currentPlayerIndex = 0)
        game = game.updateCurrentPlayer(player1)
        repository.saveGame(game)

        // Act
        bustTurnUseCase()

        // Assert – counter is 1 and no penalty (score unchanged from committed value)
        val updatedGame = repository.getCurrentGame().getOrThrow()
        val updatedPlayer = updatedGame.players[0]
        assertEquals(1, updatedPlayer.consecutiveBusts.value)
        assertEquals(700, updatedPlayer.totalScore.value) // 500 + 200, penalty not triggered
    }

    private fun createGameWithTwoPlayers(): Game {
        val player1 = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"))
        val player2 = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        return Game(
            id = GameId.of("game1"),
            players = listOf(player1, player2),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 1
        )
    }

    private fun createScoreEntry(points: Int): ScoreEntry {
        return ScoreEntry(
            id = EntryId.of(UuidGenerator.generate()),
            points = Score.of(points)
        )
    }
}
