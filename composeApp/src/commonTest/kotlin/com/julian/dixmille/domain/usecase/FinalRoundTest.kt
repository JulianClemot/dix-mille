package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FinalRoundTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase
    private lateinit var bustTurnUseCase: BustTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        commitTurnUseCase = CommitTurnUseCase(repository, ScoreValidator())
        bustTurnUseCase = BustTurnUseCase(repository, ScoreValidator())
    }

    // ── Test 1 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should trigger final round when player reaches target score`() = runTest {
        // Arrange – Alice at 9500, 500-point turn, 3 players
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500)))
        )
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(9500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        val carol = Player(id = PlayerId.of("p3"), name = PlayerName.of("Carol"))
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.FINAL_ROUND, saved.gamePhase)
        assertEquals("p1", saved.triggeringPlayerId?.value)
        assertEquals(1, saved.currentPlayerIndex) // advanced to Bob
    }

    // ── Test 2 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should set hasPlayedFinalRound when player commits during final round`() = runTest {
        // Arrange – FINAL_ROUND game, Alice is triggering, Bob is current with a turn
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500)))
        )
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val carol = Player(id = PlayerId.of("p3"), name = PlayerName.of("Carol"))
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
            createdAt = 0L,
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        val bob2 = saved.players.first { it.id.value == "p2" }
        assertTrue(bob2.hasPlayedFinalRound)
    }

    // ── Test 3 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should end game when all non-triggering players have played`() = runTest {
        // Arrange – FINAL_ROUND, Alice triggering, Bob already played, Carol is current
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500)))
        )
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            hasPlayedFinalRound = true
        )
        val carol = Player(
            id = PlayerId.of("p3"),
            name = PlayerName.of("Carol"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 2,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
            createdAt = 0L,
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, saved.gamePhase)
    }

    // ── Test 4 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should skip triggering player when advancing during final round`() = runTest {
        // Arrange – Alice (triggering, index 0), Bob (index 1, already played), Carol (index 2, current)
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500)))
        )
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            hasPlayedFinalRound = true
        )
        val carol = Player(
            id = PlayerId.of("p3"),
            name = PlayerName.of("Carol"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 2,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
            createdAt = 0L,
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        val result = commitTurnUseCase()

        // Assert – all non-triggering players have played, game ends (Alice is not given another turn)
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, saved.gamePhase)
    }

    // ── Test 5 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should end game immediately when final round is disabled`() = runTest {
        // Arrange – final round disabled, Alice at 9500 with 500-point turn
        val rules = GameRules.DEFAULT.copy(enableFinalRound = false)
        val turn = Turn(
            id = TurnId.of(UuidGenerator.generate()),
            entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(500)))
        )
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(9500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            roundNumber = 1,
            rules = rules
        )
        repository.saveGame(game)

        // Act
        val result = commitTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, saved.gamePhase)
    }

    // ── Test 6 ────────────────────────────────────────────────────────────────

    @Test
    fun `Should set hasPlayedFinalRound when player busts during final round`() = runTest {
        // Arrange – FINAL_ROUND, Alice triggering, Bob is current with an empty turn
        val turn = Turn(id = TurnId.of(UuidGenerator.generate()))
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.of(500),
            hasEnteredGame = true,
            currentTurn = turn
        )
        val carol = Player(id = PlayerId.of("p3"), name = PlayerName.of("Carol"))
        val game = Game(
            id = GameId.of("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
            createdAt = 0L,
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        val result = bustTurnUseCase()

        // Assert
        assertTrue(result.isSuccess)
        val saved = repository.getCurrentGame().getOrThrow()
        val bob2 = saved.players.first { it.id.value == "p2" }
        assertTrue(bob2.hasPlayedFinalRound)
        // Turn should advance to Carol (index 2)
        assertEquals(2, saved.currentPlayerIndex)
        assertNotNull(saved.players[2].currentTurn)
    }
}
