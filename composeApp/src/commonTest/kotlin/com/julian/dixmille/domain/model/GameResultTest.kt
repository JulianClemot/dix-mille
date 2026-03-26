package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameResult
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.event.DomainEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameResultTest {

    private fun makeGame(): Game = Game(
        id = "game-1",
        players = listOf(
            Player(id = "p1", name = "Alice"),
            Player(id = "p2", name = "Bob"),
        ),
        targetScore = 10_000,
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
            DomainEvent.TurnCommitted(playerId = "p1", points = 500, newTotalScore = 500),
            DomainEvent.PlayerEnteredGame(playerId = "p1"),
        )

        val result = GameResult(game = game, events = events)

        assertEquals(game, result.game)
        assertEquals(2, result.events.size)
        assertEquals(events, result.events)
    }

    @Test
    fun `Should be equal when game and events are equal`() {
        val game = makeGame()
        val events = listOf(DomainEvent.GameEnded(winnerId = "p1"))

        val result1 = GameResult(game = game, events = events)
        val result2 = GameResult(game = game, events = events)

        assertEquals(result1, result2)
    }
}
