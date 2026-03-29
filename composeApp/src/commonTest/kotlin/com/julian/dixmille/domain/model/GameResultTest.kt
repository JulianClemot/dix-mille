package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameResult
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.event.DomainEvent
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameResultTest {

    private fun makeGame(): Game = Game(
        id = GameId.of("game-1"),
        players = listOf(
            Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice")),
            Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob")),
        ),
        targetScore = TargetScore.of(10_000),
        createdAt = 0L,
    )

    @Test
    fun `Should hold game and empty events list by default`() {
        val game = makeGame()
        val result = GameResult(game = game)

        assertEquals(game, result.game)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `Should hold game and provided events list`() {
        val game = makeGame()
        val events = listOf(
            DomainEvent.TurnCommitted(
                playerId = PlayerId.of("p1"),
                points = Score.of(500),
                newTotalScore = Score.of(500),
            ),
            DomainEvent.PlayerEnteredGame(playerId = PlayerId.of("p1")),
        )

        val result = GameResult(game = game, events = events)

        assertEquals(game, result.game)
        assertEquals(2, result.events.size)
        assertEquals(events, result.events)
    }

    @Test
    fun `Should be equal when game and events are equal`() {
        val game = makeGame()
        val events = listOf(DomainEvent.GameEnded(winnerId = PlayerId.of("p1")))

        val result1 = GameResult(game = game, events = events)
        val result2 = GameResult(game = game, events = events)

        assertEquals(result1, result2)
    }
}
