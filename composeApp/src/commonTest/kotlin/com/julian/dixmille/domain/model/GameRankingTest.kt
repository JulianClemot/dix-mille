package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.RankedPlayer
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRankingTest {

    // ---------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------

    private fun makePlayer(id: String, name: String, totalScore: Int): Player =
        Player(
            id = PlayerId(id),
            name = PlayerName(name),
            totalScore = Score(totalScore),
            hasEnteredGame = totalScore > 0
        )

    private fun makeCrossingRecord(
        playerId: String,
        previousScore: Int,
        points: Int,
        roundNumber: Int = 1
    ): TurnRecord = TurnRecord(
        roundNumber = roundNumber,
        playerId = PlayerId(playerId),
        points = Score(points),
        outcome = TurnOutcome.SCORED,
        previousScore = Score(previousScore)
    )

    private fun makeGame(
        players: List<Player>,
        turnHistory: List<TurnRecord> = emptyList(),
        targetScore: Int = 10_000
    ): Game = Game(
        id = GameId("game1"),
        players = players,
        targetScore = TargetScore(targetScore),
        currentPlayerIndex = 0,
        gamePhase = GamePhase.ENDED,
        createdAt = 0L,
        turnHistory = turnHistory,
        roundNumber = 2,
        rules = GameRules()
    )

    // ---------------------------------------------------------------------------
    // Test 1: Single target-reaching player ranked first, others by score
    // ---------------------------------------------------------------------------

    @Test
    fun `Should rank triggering player first and others by score`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val carol = makePlayer("p3", "Carol", 7_000)
        val aliceCrossingRecord = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossingRecord),
            targetScore = 10_000
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(3, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(RankedPlayer(bob, 2), ranking[1])
        assertEquals(RankedPlayer(carol, 3), ranking[2])
    }

    // ---------------------------------------------------------------------------
    // Test 2: Multiple target-reaching players ranked by turn order in history
    // ---------------------------------------------------------------------------

    @Test
    fun `Should rank target-reaching players by turn order`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 10_500)
        val carol = makePlayer("p3", "Carol", 8_500)
        // Alice crossed at history index 0, Bob at history index 1
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500, roundNumber = 1)
        val bobCrossing = makeCrossingRecord(playerId = "p2", previousScore = 9_500, points = 1_000, roundNumber = 1)
        val game = makeGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossing, bobCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(3, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(RankedPlayer(bob, 2), ranking[1])
        assertEquals(RankedPlayer(carol, 3), ranking[2])
    }

    // ---------------------------------------------------------------------------
    // Test 3: Player who crossed target earlier ranks above player who crossed later
    // ---------------------------------------------------------------------------

    @Test
    fun `Should rank player who crossed target earlier above player who crossed later`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_500)
        val bob = makePlayer("p2", "Bob", 10_000)
        // Alice's crossing record at index 0, Bob's at index 1
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 1_000)
        val bobCrossing = makeCrossingRecord(playerId = "p2", previousScore = 9_500, points = 500)
        val game = makeGame(
            players = listOf(alice, bob),
            turnHistory = listOf(aliceCrossing, bobCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(2, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(RankedPlayer(bob, 2), ranking[1])
    }

    // ---------------------------------------------------------------------------
    // Test 4: Tied players below target share the same rank
    // ---------------------------------------------------------------------------

    @Test
    fun `Should assign same rank to tied players below target`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val carol = makePlayer("p3", "Carol", 8_500)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(3, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(2, ranking[1].rank)
        assertEquals(2, ranking[2].rank)
    }

    // ---------------------------------------------------------------------------
    // Test 5: Rank number skips after tied players (standard competition ranking)
    // ---------------------------------------------------------------------------

    @Test
    fun `Should skip rank number after tied players`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val carol = makePlayer("p3", "Carol", 8_500)
        val dave = makePlayer("p4", "Dave", 6_000)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeGame(
            players = listOf(alice, bob, carol, dave),
            turnHistory = listOf(aliceCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(4, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(2, ranking[1].rank)
        assertEquals(2, ranking[2].rank)
        assertEquals(RankedPlayer(dave, 4), ranking[3])
    }

    // ---------------------------------------------------------------------------
    // Test 6: Target-reaching players always rank above below-target players
    // ---------------------------------------------------------------------------

    @Test
    fun `Should always rank target-reaching players above below-target players`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 11_000)
        val carol = makePlayer("p3", "Carol", 9_800)
        // Alice crossed target at index 0, Bob at index 1
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500, roundNumber = 1)
        val bobCrossing = makeCrossingRecord(playerId = "p2", previousScore = 9_500, points = 1_500, roundNumber = 2)
        val game = makeGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossing, bobCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(3, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(RankedPlayer(bob, 2), ranking[1])
        assertEquals(RankedPlayer(carol, 3), ranking[2])
    }

    // ---------------------------------------------------------------------------
    // Test 7: All players below target — ranked by score descending with ties
    // ---------------------------------------------------------------------------

    @Test
    fun `Should rank all players by score descending when no one reached target`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 8_000)
        val bob = makePlayer("p2", "Bob", 7_000)
        val carol = makePlayer("p3", "Carol", 7_000)
        val game = makeGame(
            players = listOf(alice, bob, carol),
            turnHistory = emptyList()
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(3, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(2, ranking[1].rank)
        assertEquals(2, ranking[2].rank)
    }

    // ---------------------------------------------------------------------------
    // Test 8: Mixed ranking — two target-reaching + two tied below-target
    // ---------------------------------------------------------------------------

    @Test
    fun `Should produce correct mixed ranking with ties`() {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val carol = makePlayer("p3", "Carol", 10_500)
        val bob = makePlayer("p2", "Bob", 8_500)
        val dave = makePlayer("p4", "Dave", 8_500)
        // Alice crosses at history index 0, Carol at history index 2 (index 1 is something else)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500, roundNumber = 1)
        val someTurn = TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("p2"),
            points = Score(500),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(8_000)
        )
        val carolCrossing = makeCrossingRecord(playerId = "p3", previousScore = 9_500, points = 1_000, roundNumber = 2)
        val game = makeGame(
            players = listOf(alice, carol, bob, dave),
            turnHistory = listOf(aliceCrossing, someTurn, carolCrossing)
        )

        // Act
        val ranking = game.getRanking()

        // Assert
        assertEquals(4, ranking.size)
        assertEquals(RankedPlayer(alice, 1), ranking[0])
        assertEquals(RankedPlayer(carol, 2), ranking[1])
        assertEquals(3, ranking[2].rank)
        assertEquals(3, ranking[3].rank)
    }
}
