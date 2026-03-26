package com.julian.dixmille.domain.model.event

import com.julian.dixmille.core.domain.model.event.DomainEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class DomainEventTest {

    @Test
    fun `Should create TurnCommitted with correct fields`() {
        val event = DomainEvent.TurnCommitted(
            playerId = "player-1",
            points = 500,
            newTotalScore = 1500,
        )

        assertEquals("player-1", event.playerId)
        assertEquals(500, event.points)
        assertEquals(1500, event.newTotalScore)
    }

    @Test
    fun `Should create PlayerBusted with correct playerId`() {
        val event = DomainEvent.PlayerBusted(playerId = "player-2")

        assertEquals("player-2", event.playerId)
    }

    @Test
    fun `Should create TurnSkipped with correct playerId`() {
        val event = DomainEvent.TurnSkipped(playerId = "player-3")

        assertEquals("player-3", event.playerId)
    }

    @Test
    fun `Should create PlayerEnteredGame with correct playerId`() {
        val event = DomainEvent.PlayerEnteredGame(playerId = "player-4")

        assertEquals("player-4", event.playerId)
    }

    @Test
    fun `Should create FinalRoundStarted with correct triggeringPlayerId`() {
        val event = DomainEvent.FinalRoundStarted(triggeringPlayerId = "player-5")

        assertEquals("player-5", event.triggeringPlayerId)
    }

    @Test
    fun `Should create GameEnded with correct winnerId`() {
        val event = DomainEvent.GameEnded(winnerId = "player-6")

        assertEquals("player-6", event.winnerId)
    }

    @Test
    fun `Should distinguish different event subtypes`() {
        val committed = DomainEvent.TurnCommitted(playerId = "p1", points = 500, newTotalScore = 500)
        val busted = DomainEvent.PlayerBusted(playerId = "p1")
        val skipped = DomainEvent.TurnSkipped(playerId = "p1")
        val entered = DomainEvent.PlayerEnteredGame(playerId = "p1")
        val finalRound = DomainEvent.FinalRoundStarted(triggeringPlayerId = "p1")
        val ended = DomainEvent.GameEnded(winnerId = "p1")

        assertIs<DomainEvent.TurnCommitted>(committed)
        assertIs<DomainEvent.PlayerBusted>(busted)
        assertIs<DomainEvent.TurnSkipped>(skipped)
        assertIs<DomainEvent.PlayerEnteredGame>(entered)
        assertIs<DomainEvent.FinalRoundStarted>(finalRound)
        assertIs<DomainEvent.GameEnded>(ended)

        assertNotEquals<DomainEvent>(committed, busted)
        assertNotEquals<DomainEvent>(busted, skipped)
    }
}
