package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameFinalRoundExceedTargetTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        commitTurnUseCase = CommitTurnUseCase(repository, ScoreValidator())
    }

    // ---------- helpers ----------

    private fun createScoreEntry(points: Int): ScoreEntry =
        ScoreEntry(id = EntryId(UuidGenerator.generate()), points = Score(points))

    /**
     * Builds a 2-player FINAL_ROUND game:
     * Alice (triggering) at 10_000, Bob (current) at [bobScore] with a single
     * turn entry worth [bobTurnPoints].
     */
    private suspend fun setupTwoPlayerGame(
        bobScore: Int,
        bobTurnPoints: Int,
        target: Int = 10_000
    ): Game {
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(target),
            hasEnteredGame = true
        )
        var bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(bobScore),
            hasEnteredGame = true
        )
        bob = bob.startTurn(TurnId(UuidGenerator.generate()))
        bob = bob.addScoreEntry(createScoreEntry(bobTurnPoints))

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(target),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)
        return game
    }

    // ---------- test 1 ----------

    @Test
    fun `Should not change player score when committed total exceeds target in FINAL_ROUND`() = runTest {
        // Arrange: Bob at 9800, turn = 600 → projected 10400 > 10000
        setupTwoPlayerGame(bobScore = 9800, bobTurnPoints = 600)

        // Act
        commitTurnUseCase()

        // Assert: Bob's score unchanged
        val result = repository.getCurrentGame().getOrThrow()
        val bob = result.players.first { it.id.value == "p2" }
        assertEquals(9800, bob.totalScore.value)
    }

    // ---------- test 2 ----------

    @Test
    fun `Should record BUST in turn history when committed total exceeds target in FINAL_ROUND`() = runTest {
        // Arrange: Bob at 9800, turn = 600 → projected 10400 > 10000
        setupTwoPlayerGame(bobScore = 9800, bobTurnPoints = 600)

        // Act
        commitTurnUseCase()

        // Assert: last TurnRecord is BUST with Score.ZERO
        val result = repository.getCurrentGame().getOrThrow()
        val lastRecord = result.turnHistory.last { it.playerId.value == "p2" }
        assertEquals(TurnOutcome.BUST, lastRecord.outcome)
        assertEquals(0, lastRecord.points.value)
    }

    // ---------- test 3 ----------

    @Test
    fun `Should increment consecutive busts when committed total exceeds target in FINAL_ROUND`() = runTest {
        // Arrange: Bob starts with 0 consecutive busts
        setupTwoPlayerGame(bobScore = 9800, bobTurnPoints = 600)

        // Act
        commitTurnUseCase()

        // Assert: consecutiveBusts == 1
        val result = repository.getCurrentGame().getOrThrow()
        val bob = result.players.first { it.id.value == "p2" }
        assertEquals(1, bob.consecutiveBusts.value)
    }

    // ---------- test 4 ----------

    @Test
    fun `Should mark hasPlayedFinalRound true when bust-by-exceed in FINAL_ROUND`() = runTest {
        // Arrange: Bob's hasPlayedFinalRound starts false
        setupTwoPlayerGame(bobScore = 9800, bobTurnPoints = 600)

        // Act
        commitTurnUseCase()

        // Assert: hasPlayedFinalRound is true
        val result = repository.getCurrentGame().getOrThrow()
        val bob = result.players.first { it.id.value == "p2" }
        assertEquals(true, bob.hasPlayedFinalRound)
    }

    // ---------- test 5 ----------

    @Test
    fun `Should commit normally when turn total reaches target exactly in FINAL_ROUND`() = runTest {
        // Arrange: Bob at 9000, turn = 1000 → projected 10000 == 10000 (valid)
        setupTwoPlayerGame(bobScore = 9000, bobTurnPoints = 1000)

        // Act
        commitTurnUseCase()

        // Assert: Bob's score is 10000, last record is SCORED
        val result = repository.getCurrentGame().getOrThrow()
        val bobRecord = result.turnHistory.last { it.playerId.value == "p2" }
        assertEquals(TurnOutcome.SCORED, bobRecord.outcome)
        // Find Bob in the final game players list by searching turn history for his playerId
        // Bob may have been replaced by the next player if game ended; check turn history score
        // The previous players list before game ended can be cross-checked with turn records.
        // Since the game ended (Bob is last player), check the ENDED game's player scores.
        val finalBob = result.players.first { it.id.value == "p2" }
        assertEquals(10000, finalBob.totalScore.value)
    }

    // ---------- test 6 ----------

    @Test
    fun `Should treat as bust when turn total is one point over target in FINAL_ROUND`() = runTest {
        // Arrange: Bob at 9000, turn = 1001 → projected 10001 > 10000
        // Note: Score must be a multiple of 50. 1050 is the closest valid value over 1000.
        // Use 1050 so projected = 10050 > 10000
        setupTwoPlayerGame(bobScore = 9000, bobTurnPoints = 1050)

        // Act
        commitTurnUseCase()

        // Assert: Bob's score stays 9000, BUST recorded
        val result = repository.getCurrentGame().getOrThrow()
        val bob = result.players.first { it.id.value == "p2" }
        assertEquals(9000, bob.totalScore.value)
        val bobRecord = result.turnHistory.last { it.playerId.value == "p2" }
        assertEquals(TurnOutcome.BUST, bobRecord.outcome)
    }

    // ---------- test 7 ----------

    @Test
    fun `Should advance to next player after bust-by-exceed in FINAL_ROUND`() = runTest {
        // Arrange: 3 players — Alice (triggering, p1, idx 0) at 10000,
        // Bob (current, p2, idx 1) at 9800 with 600-pt turn (projects to 10400),
        // Carol (p3, idx 2) hasn't played yet
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10000),
            hasEnteredGame = true
        )
        var bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(9800),
            hasEnteredGame = true
        )
        bob = bob.startTurn(TurnId(UuidGenerator.generate()))
        bob = bob.addScoreEntry(createScoreEntry(600))

        val carol = Player(
            id = PlayerId("p3"),
            name = PlayerName("Carol"),
            totalScore = Score(5000),
            hasEnteredGame = true
        )

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob, carol),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(10000),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: current player is Carol (p3) and she has a non-null currentTurn
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals("p3", result.currentPlayer.id.value)
        assertEquals(true, result.currentPlayer.currentTurn != null)
    }

    // ---------- test 8 ----------

    @Test
    fun `Should end game when last non-triggering player busts-by-exceed in FINAL_ROUND`() = runTest {
        // Arrange: 2 players — Alice (triggering, p1) at 10000,
        // Bob (only non-triggering, p2) at 9800 with 600-pt turn (projects to 10400)
        // Bob is the last (and only) non-triggering player, so game should end
        setupTwoPlayerGame(bobScore = 9800, bobTurnPoints = 600)

        // Act
        commitTurnUseCase()

        // Assert: game phase is ENDED
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, result.gamePhase)
    }
}
