package com.julian.dixmille.domain.model.event

import com.julian.dixmille.core.domain.model.event.DomainEvent
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class DomainEventTest {

    @Test
    fun `Should create TurnCommitted with PlayerId and Score fields`() {
        val event = DomainEvent.TurnCommitted(
            playerId = PlayerId("player-1"),
            points = Score(500),
            newTotalScore = Score(1500),
        )

        assertEquals(PlayerId("player-1"), event.playerId)
        assertEquals(500, event.points.value)
        assertEquals(1500, event.newTotalScore.value)
    }

    @Test
    fun `Should create PlayerBusted with correct playerId`() {
        val event = DomainEvent.PlayerBusted(playerId = PlayerId("player-2"))

        assertEquals(PlayerId("player-2"), event.playerId)
    }

    @Test
    fun `Should create TurnSkipped with correct playerId`() {
        val event = DomainEvent.TurnSkipped(playerId = PlayerId("player-3"))

        assertEquals(PlayerId("player-3"), event.playerId)
    }

    @Test
    fun `Should create PlayerEnteredGame with correct playerId`() {
        val event = DomainEvent.PlayerEnteredGame(playerId = PlayerId("player-4"))

        assertEquals(PlayerId("player-4"), event.playerId)
    }

    @Test
    fun `Should create FinalRoundStarted with correct triggeringPlayerId`() {
        val event = DomainEvent.FinalRoundStarted(triggeringPlayerId = PlayerId("player-5"))

        assertEquals(PlayerId("player-5"), event.triggeringPlayerId)
    }

    @Test
    fun `Should create GameEnded with PlayerId winnerId`() {
        val event = DomainEvent.GameEnded(winnerId = PlayerId("player-6"))

        assertEquals(PlayerId("player-6"), event.winnerId)
    }

    @Test
    fun `Should distinguish different event subtypes`() {
        val committed = DomainEvent.TurnCommitted(
            playerId = PlayerId("p1"),
            points = Score(500),
            newTotalScore = Score(500),
        )
        val busted = DomainEvent.PlayerBusted(playerId = PlayerId("p1"))
        val skipped = DomainEvent.TurnSkipped(playerId = PlayerId("p1"))
        val entered = DomainEvent.PlayerEnteredGame(playerId = PlayerId("p1"))
        val finalRound = DomainEvent.FinalRoundStarted(triggeringPlayerId = PlayerId("p1"))
        val ended = DomainEvent.GameEnded(winnerId = PlayerId("p1"))

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
