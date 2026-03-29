package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.event.DomainEvent
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameAggregateEventsTest {

    // ---------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------

    private fun makeGame(
        players: List<Player> = listOf(
            Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice")),
            Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob")),
        ),
        targetScore: Int = 10_000,
        gamePhase: GamePhase = GamePhase.IN_PROGRESS,
        triggeringPlayerId: PlayerId? = null,
        rules: GameRules = GameRules(),
        currentPlayerIndex: Int = 0,
    ): Game = Game(
        id = GameId.of("game-1"),
        players = players,
        targetScore = TargetScore.of(targetScore),
        gamePhase = gamePhase,
        triggeringPlayerId = triggeringPlayerId,
        createdAt = 0L,
        rules = rules,
        currentPlayerIndex = currentPlayerIndex,
    )

    // ---------------------------------------------------------------------------
    // checkAndTriggerFinalRound
    // ---------------------------------------------------------------------------

    @Test
    fun `Should emit FinalRoundStarted event when player reaches target score`() {
        // Arrange
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000), hasEnteredGame = true)
        val game = makeGame(players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))))

        // Act
        val result = game.checkAndTriggerFinalRound()

        // Assert
        assertEquals(GamePhase.FINAL_ROUND, result.game.gamePhase)
        assertEquals(1, result.events.size)
        val event = result.events.first()
        assertTrue(event is DomainEvent.FinalRoundStarted)
        assertEquals("p1", event.triggeringPlayerId.value)
    }

    @Test
    fun `Should not emit FinalRoundStarted event when player is below target score`() {
        // Arrange
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(9_000), hasEnteredGame = true)
        val game = makeGame(players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))))

        // Act
        val result = game.checkAndTriggerFinalRound()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.game.gamePhase)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `Should return empty events when game is already in final round on checkAndTriggerFinalRound`() {
        // Arrange
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
        )

        // Act
        val result = game.checkAndTriggerFinalRound()

        // Assert
        assertEquals(GamePhase.FINAL_ROUND, result.game.gamePhase)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `Should emit GameEnded event when final round is disabled and player reaches target score`() {
        // Arrange
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000), hasEnteredGame = true)
        val game = makeGame(
            players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            rules = GameRules(enableFinalRound = false),
        )

        // Act
        val result = game.checkAndTriggerFinalRound()

        // Assert
        assertEquals(GamePhase.ENDED, result.game.gamePhase)
        assertEquals(1, result.events.size)
        val event = result.events.first()
        assertTrue(event is DomainEvent.GameEnded)
        assertEquals("p1", event.winnerId.value)
    }

    // ---------------------------------------------------------------------------
    // checkAndEndGame
    // ---------------------------------------------------------------------------

    @Test
    fun `Should emit GameEnded event when all final round players have played`() {
        // Arrange: p2 has played final round; p1 is triggering player
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000), hasEnteredGame = true)
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(8_000), hasEnteredGame = true, hasPlayedFinalRound = true)
        val game = makeGame(
            players = listOf(alice, bob),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
        )

        // Act
        val result = game.checkAndEndGame()

        // Assert
        assertEquals(GamePhase.ENDED, result.game.gamePhase)
        assertEquals(1, result.events.size)
        val event = result.events.first()
        assertTrue(event is DomainEvent.GameEnded)
        // Alice has higher score so she is winner
        assertEquals("p1", event.winnerId.value)
    }

    @Test
    fun `Should not emit GameEnded event when not all final round players have played`() {
        // Arrange: p2 has NOT played final round yet
        val alice = Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice"), totalScore = Score.of(10_000), hasEnteredGame = true)
        val bob = Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"), totalScore = Score.of(8_000), hasEnteredGame = true, hasPlayedFinalRound = false)
        val game = makeGame(
            players = listOf(alice, bob),
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId.of("p1"),
        )

        // Act
        val result = game.checkAndEndGame()

        // Assert
        assertEquals(GamePhase.FINAL_ROUND, result.game.gamePhase)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `Should return empty events when game has not ended on checkAndEndGame`() {
        // Arrange: game is still in progress (not in final round)
        val game = makeGame()

        // Act
        val result = game.checkAndEndGame()

        // Assert
        assertEquals(GamePhase.IN_PROGRESS, result.game.gamePhase)
        assertTrue(result.events.isEmpty())
    }
}
