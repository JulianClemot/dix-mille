package com.julian.dixmille.core.domain.model.event

sealed class DomainEvent {
    data class TurnCommitted(
        val playerId: String,
        val points: Int,
        val newTotalScore: Int,
    ) : DomainEvent()

    data class PlayerBusted(val playerId: String) : DomainEvent()

    data class TurnSkipped(val playerId: String) : DomainEvent()

    data class PlayerEnteredGame(val playerId: String) : DomainEvent()

    data class FinalRoundStarted(val triggeringPlayerId: String) : DomainEvent()

    data class GameEnded(val winnerId: String) : DomainEvent()
}
