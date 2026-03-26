package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.event.DomainEvent

data class GameResult(
    val game: Game,
    val events: List<DomainEvent> = emptyList(),
)
