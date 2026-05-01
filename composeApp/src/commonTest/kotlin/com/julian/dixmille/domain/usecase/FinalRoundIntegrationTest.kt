package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
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
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Integration tests for the final round system.
 *
 * These tests exercise multi-layer interactions that unit tests cannot cover:
 * - CommitTurnUseCase ↔ Game.checkAndTriggerFinalRound()
 * - UndoLastTurnUseCase ↔ Game.undoLastTurn() ↔ GameRepository
 * - resolveScoreCollisions guard in FINAL_ROUND vs IN_PROGRESS
 * - getRanking() using turnHistory recorded by CommitTurnUseCase
 */
class FinalRoundIntegrationTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase
    private lateinit var undoLastTurnUseCase: UndoLastTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        commitTurnUseCase = CommitTurnUseCase(repository, ScoreValidator())
        undoLastTurnUseCase = UndoLastTurnUseCase(repository)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun turnWith(vararg points: Int): Turn = Turn(
        id = TurnId(UuidGenerator.generate()),
        entries = points.map { ScoreEntry(id = EntryId(UuidGenerator.generate()), points = Score(it)) }
    )

    // ── Test 1: Full final-round-then-undo flow ────────────────────────────────

    /**
     * CommitTurnUseCase triggers FINAL_ROUND when a player's commit crosses the target.
     * Immediately calling UndoLastTurnUseCase must revert back to IN_PROGRESS, restore the
     * triggering player's score, and give them a fresh turn.
     *
     * Why a unit test can't catch this: undoLastTurn() unit tests build game state manually.
     * This test verifies that the turnHistory written by CommitTurnUseCase (with the correct
     * previousScore / points values) causes undoLastTurn() to detect the triggering turn correctly.
     */
    @Test
    fun `Should revert FINAL_ROUND to IN_PROGRESS when undoing triggering commit`() = runTest {
        // Arrange — Alice at 9500, commits 500-pt turn → triggers FINAL_ROUND
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(9_500),
            hasEnteredGame = true,
            currentTurn = turnWith(500)
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"))
        val game = Game(
            id = GameId("g1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
        repository.saveGame(game)

        commitTurnUseCase()

        // Verify FINAL_ROUND was triggered
        val afterCommit = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.FINAL_ROUND, afterCommit.gamePhase)

        // We need to set up a state where we can undo — the current player (Bob) needs a turn
        // (CommitTurnUseCase already did this), so just call undo
        undoLastTurnUseCase()

        // Assert
        val afterUndo = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.IN_PROGRESS, afterUndo.gamePhase)
        assertNull(afterUndo.triggeringPlayerId)
        val undoneAlice = afterUndo.players.first { it.id == PlayerId("p1") }
        assertEquals(9_500, undoneAlice.totalScore.value)
    }

    // ── Test 2: Exceed-target bust does not trigger second final round ──────────

    /**
     * During FINAL_ROUND, committing a turn whose projected total exceeds the target must
     * be treated as a bust — no second FINAL_ROUND or ENDED transition should happen until
     * all non-triggering players have played.
     *
     * Why a unit test can't catch this: the early-return bust path in CommitTurnUseCase
     * and checkAndTriggerFinalRound are only visible together at the use-case level.
     */
    @Test
    fun `Should bust but stay in FINAL_ROUND when projected total exceeds target`() = runTest {
        // Arrange — FINAL_ROUND, Alice triggered (10000), Bob has a 1000-pt turn (9500+1000=10500 > 10000)
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10_000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(9_500),
            hasEnteredGame = true,
            currentTurn = turnWith(1_000)
        )
        val carol = Player(id = PlayerId("p3"), name = PlayerName("Carol"))
        val game = Game(
            id = GameId("g1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L
        )
        repository.saveGame(game)

        val result = commitTurnUseCase()

        // Assert
        val saved = repository.getCurrentGame().getOrThrow()
        assertEquals(true, result.isSuccess)
        assertEquals(GamePhase.FINAL_ROUND, saved.gamePhase) // still in final round
        val savedBob = saved.players.first { it.id == PlayerId("p2") }
        assertEquals(9_500, savedBob.totalScore.value) // score unchanged (bust, not commit)
        assertEquals(true, savedBob.hasPlayedFinalRound)
    }

    // ── Test 3: Full game end produces correct ranking order ──────────────────

    /**
     * After the full game ends via CommitTurnUseCase, calling getRanking() on the saved game
     * must return players in the correct order: at-target players sorted by crossing order,
     * below-target players sorted by score descending.
     *
     * Why a unit test can't catch this: GameRankingTest builds turnHistory manually. This test
     * verifies that the previousScore/points values recorded by CommitTurnUseCase are correct
     * enough for getRanking() to detect the target crossing accurately.
     */
    @Test
    fun `Should rank players correctly using turnHistory built by CommitTurnUseCase`() = runTest {
        // Arrange — Alice crosses target first (9500 + 500), then Bob commits (9000 + 1500 = 10500 > target
        // but wait, Bob exceeds target so it's a bust). Let's simplify:
        // Alice: 9500 -> commits 500 -> 10000 (crosses target, FINAL_ROUND starts)
        // Bob: 8000, commits 500 -> 8500 (below target, final round done)
        // Carol: 7000, commits 500 -> 7500 (below target, final round done, game ends)
        // Expected ranking: Alice(1), Bob(2), Carol(3)
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(9_500),
            hasEnteredGame = true,
            currentTurn = turnWith(500)
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(8_000),
            hasEnteredGame = true
        )
        val carol = Player(
            id = PlayerId("p3"),
            name = PlayerName("Carol"),
            totalScore = Score(7_000),
            hasEnteredGame = true
        )
        val game = Game(
            id = GameId("g1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
        repository.saveGame(game)

        // Alice commits → FINAL_ROUND, moves to Bob
        commitTurnUseCase()

        // Bob needs a turn
        val afterAlice = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.FINAL_ROUND, afterAlice.gamePhase)
        assertEquals(1, afterAlice.currentPlayerIndex) // Bob's turn

        // Set Bob's turn
        val bobWithTurn = afterAlice.currentPlayer.startTurn(TurnId(UuidGenerator.generate()))
            .copy(currentTurn = turnWith(500))
        repository.saveGame(afterAlice.updateCurrentPlayer(bobWithTurn))

        commitTurnUseCase() // Bob commits 500 → 8500, hasPlayedFinalRound=true

        val afterBob = repository.getCurrentGame().getOrThrow()
        assertEquals(2, afterBob.currentPlayerIndex) // Carol's turn

        // Set Carol's turn
        val carolWithTurn = afterBob.currentPlayer.startTurn(TurnId(UuidGenerator.generate()))
            .copy(currentTurn = turnWith(500))
        repository.saveGame(afterBob.updateCurrentPlayer(carolWithTurn))

        commitTurnUseCase() // Carol commits 500 → 7500, all played → game ENDS

        val ended = repository.getCurrentGame().getOrThrow()
        assertEquals(GamePhase.ENDED, ended.gamePhase)

        // Assert ranking
        val ranking = ended.getRanking()
        assertEquals(3, ranking.size)
        assertEquals(PlayerId("p1"), ranking[0].player.id) // Alice: at target, crossed first
        assertEquals(1, ranking[0].rank)
        assertEquals(PlayerId("p2"), ranking[1].player.id) // Bob: 8500
        assertEquals(2, ranking[1].rank)
        assertEquals(PlayerId("p3"), ranking[2].player.id) // Carol: 7500
        assertEquals(3, ranking[2].rank)
    }

    // ── Test 4: Collision guard — IN_PROGRESS vs FINAL_ROUND symmetry ──────────

    /**
     * IN_PROGRESS: scoring to match another player's score fires a collision (victim reverts).
     * FINAL_ROUND: scoring to match another player's score does NOT fire a collision.
     *
     * Why a unit test can't catch this: GameFinalRoundCollisionTest tests the guard on Game
     * directly. This test verifies the full commit → save → reload cycle through the repository
     * for both branches, confirming the guard is in the right place in CommitTurnUseCase.
     */
    @Test
    fun `Should fire collision in IN_PROGRESS but not in FINAL_ROUND`() = runTest {
        // ── Part A: IN_PROGRESS collision ────────────────────────────────────
        val aliceA = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(500),
            hasEnteredGame = true,
            currentTurn = turnWith(500) // commits to 1000, matching Bob
        )
        val bobA = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(1_000),
            hasEnteredGame = true
        )
        val gameA = Game(
            id = GameId("gA"),
            players = listOf(aliceA, bobA),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p2"),
                    points = Score(1_000),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score.ZERO
                )
            )
        )
        repository.saveGame(gameA)
        commitTurnUseCase()

        val savedA = repository.getCurrentGame().getOrThrow()
        val savedBobA = savedA.players.first { it.id == PlayerId("p2") }
        // Bob should have been pushed back to 0 (his previousScore before his 1000-pt turn)
        assertEquals(0, savedBobA.totalScore.value, "Collision should revert Bob in IN_PROGRESS")

        // ── Part B: FINAL_ROUND no collision ─────────────────────────────────
        val aliceB = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10_000),
            hasEnteredGame = true
        )
        val bobB = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(9_500),
            hasEnteredGame = true,
            currentTurn = turnWith(500) // commits to 10000, matching Alice — no collision expected
        )
        val carol = Player(id = PlayerId("p3"), name = PlayerName("Carol"))
        val gameB = Game(
            id = GameId("gB"),
            players = listOf(aliceB, bobB, carol),
            targetScore = TargetScore(10_000),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            createdAt = 0L,
            turnHistory = listOf(
                TurnRecord(
                    roundNumber = 1,
                    playerId = PlayerId("p1"),
                    points = Score(500),
                    outcome = TurnOutcome.SCORED,
                    previousScore = Score(9_500)
                )
            )
        )
        repository.saveGame(gameB)
        commitTurnUseCase()

        val savedB = repository.getCurrentGame().getOrThrow()
        val savedAliceB = savedB.players.first { it.id == PlayerId("p1") }
        val savedBobB = savedB.players.first { it.id == PlayerId("p2") }
        // Alice must keep her 10000 — no collision in FINAL_ROUND
        assertEquals(10_000, savedAliceB.totalScore.value, "Alice should keep score in FINAL_ROUND")
        assertEquals(10_000, savedBobB.totalScore.value, "Bob should keep score in FINAL_ROUND")
    }
}
