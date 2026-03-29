package com.julian.dixmille.core.domain.model.vo

import kotlinx.serialization.Serializable

@Serializable
value class TurnId private constructor(val value: String) {

    companion object {
        fun of(value: String): TurnId {
            require(value.isNotBlank()) { "TurnId value must not be blank" }
            return TurnId(value)
        }
    }
}
