package com.julian.dixmille.core.domain.model.event

import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.Score

sealed class DomainEvent {
    data class TurnCommitted(
        val playerId: PlayerId,
        val points: Score,
        val newTotalScore: Score,
    ) : DomainEvent()

    data class PlayerBusted(val playerId: PlayerId) : DomainEvent()

    data class TurnSkipped(val playerId: PlayerId) : DomainEvent()

    data class PlayerEnteredGame(val playerId: PlayerId) : DomainEvent()

    data class FinalRoundStarted(val triggeringPlayerId: PlayerId) : DomainEvent()

    data class GameEnded(val winnerId: PlayerId) : DomainEvent()
}
