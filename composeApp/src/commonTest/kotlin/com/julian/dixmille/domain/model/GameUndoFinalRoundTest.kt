package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GameUndoFinalRoundTest {

    // ---------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------

    private val alice = Player(id = PlayerId("p1"), name = PlayerName("Alice"))
    private val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"))

    private fun makeGame(
        players: List<Player> = listOf(alice, bob),
        targetScore: Int = 10_000,
        gamePhase: GamePhase = GamePhase.IN_PROGRESS,
        triggeringPlayerId: PlayerId? = null,
        turnHistory: List<TurnRecord> = emptyList(),
        currentPlayerIndex: Int = 0,
        roundNumber: Int = 1,
        rules: GameRules = GameRules(),
    ): Game = Game(
        id = GameId("game-1"),
        players = players,
        targetScore = TargetScore(targetScore),
        gamePhase = gamePhase,
        triggeringPlayerId = triggeringPlayerId,
        createdAt = 0L,
        turnHistory = turnHistory,
        currentPlayerIndex = currentPlayerIndex,
        roundNumber = roundNumber,
        rules = rules,
    )

    // ---------------------------------------------------------------------------
    // Test 1 — Regression guard: normal IN_PROGRESS undo
    // ---------------------------------------------------------------------------

    @Test
    fun `Should keep phase as IN_PROGRESS when undoing normal turn`() {
        // Arrange
        val aliceWithScore = alice.copy(totalScore = Score(500), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceWithScore, bob),
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO,
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.gamePhase)
        assertNull(result.triggeringPlayerId)
    }

    // ---------------------------------------------------------------------------
    // Test 2 — Revert phase when undoing triggering player turn
    // ---------------------------------------------------------------------------

    @Test
    fun `Should revert phase to IN_PROGRESS when undoing triggering player turn`() {
        // Arrange: Alice triggered FINAL_ROUND (was at 9500, scored 500 to reach 10000)
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bob),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_500),  // was below target
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.gamePhase)
    }

    // ---------------------------------------------------------------------------
    // Test 3 — Clear triggeringPlayerId when undoing triggering player turn
    // ---------------------------------------------------------------------------

    @Test
    fun `Should clear triggeringPlayerId when undoing triggering player turn`() {
        // Arrange: same setup as test 2
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bob),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_500),
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertNull(result.triggeringPlayerId)
    }

    // ---------------------------------------------------------------------------
    // Test 4 — Keep FINAL_ROUND when undoing non-triggering player turn
    // ---------------------------------------------------------------------------

    @Test
    fun `Should keep phase as FINAL_ROUND when undoing non-triggering player turn`() {
        // Arrange: Alice triggered, Bob played his final round turn
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val bobWithScore = bob.copy(totalScore = Score(500), hasEnteredGame = true, hasPlayedFinalRound = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bobWithScore),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            currentPlayerIndex = 0,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(10_000),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO,
                ),
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p2"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO,
                ),
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertEquals(GamePhase.FINAL_ROUND, result.gamePhase)
        assertEquals(PlayerId("p1"), result.triggeringPlayerId)
    }

    // ---------------------------------------------------------------------------
    // Test 5 — Reset hasPlayedFinalRound only for undone player
    // ---------------------------------------------------------------------------

    @Test
    fun `Should reset hasPlayedFinalRound only for undone player`() {
        // Arrange: Alice triggered (hasPlayedFinalRound irrelevant), Bob played final round
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val bobWithFinalRound = bob.copy(totalScore = Score(500), hasEnteredGame = true, hasPlayedFinalRound = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bobWithFinalRound),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            currentPlayerIndex = 0,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(10_000),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO,
                ),
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p2"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO,
                ),
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert — Bob's hasPlayedFinalRound reverts to false
        val resultBob = result.players.first { it.id.value == "p2" }
        assertEquals(false, resultBob.hasPlayedFinalRound)
        // Alice's state is unchanged
        val resultAlice = result.players.first { it.id.value == "p1" }
        assertEquals(false, resultAlice.hasPlayedFinalRound)
    }

    // ---------------------------------------------------------------------------
    // Test 6 — Boundary: triggering turn hit target exactly
    // ---------------------------------------------------------------------------

    @Test
    fun `Should revert phase when triggering turn hit target exactly`() {
        // Arrange: previousScore=9500, points=500, targetScore=10000 — exactly at target
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bob),
            targetScore = 10_000,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_500),
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.gamePhase)
    }

    // ---------------------------------------------------------------------------
    // Test 7 — Boundary: triggering turn exceeded target
    // ---------------------------------------------------------------------------

    @Test
    fun `Should revert phase when triggering turn exceeded target`() {
        // Arrange: previousScore=9600, points=600 → total=10200, targetScore=10000 — exceeded
        val aliceOverTarget = alice.copy(totalScore = Score(10_200), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceOverTarget, bob),
            targetScore = 10_000,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(600),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_600),
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.gamePhase)
    }

    // ---------------------------------------------------------------------------
    // Test 8 — Keep FINAL_ROUND when another player also reached target
    // ---------------------------------------------------------------------------

    @Test
    fun `Should keep FINAL_ROUND when another player has also reached target after undoing triggering turn`() {
        // Arrange: Alice triggered FINAL_ROUND (9500+500=10000), but Bob also has totalScore=10000
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val bobAtTarget = bob.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bobAtTarget),
            targetScore = 10_000,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_500),
                )
            ),
        )

        // Act
        val result = game.undoLastTurn()

        // Assert — Bob is still at target so game stays FINAL_ROUND
        assertEquals(GamePhase.FINAL_ROUND, result.gamePhase)
    }

    // ---------------------------------------------------------------------------
    // Test 9 — Throw when undoing with empty turn history in FINAL_ROUND
    // ---------------------------------------------------------------------------

    @Test
    fun `Should throw when undoing with empty turn history in FINAL_ROUND`() {
        // Arrange: game in FINAL_ROUND with no turn history
        val aliceAtTarget = alice.copy(totalScore = Score(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(aliceAtTarget, bob),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            turnHistory = emptyList(),
        )

        // Act + Assert — should throw same as existing empty-history behavior
        assertFailsWith<IllegalStateException> {
            game.undoLastTurn()
        }
    }
}
