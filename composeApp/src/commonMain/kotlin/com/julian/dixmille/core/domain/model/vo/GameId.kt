package com.julian.dixmille.core.domain.model.vo

value class GameId private constructor(val value: String) {

    companion object {
        fun of(value: String): GameId {
            require(value.isNotBlank()) { "GameId value must not be blank" }
            return GameId(value)
        }
    }
}
