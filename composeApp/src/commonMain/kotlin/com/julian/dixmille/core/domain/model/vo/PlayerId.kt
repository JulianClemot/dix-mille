package com.julian.dixmille.core.domain.model.vo

value class PlayerId private constructor(val value: String) {

    companion object {
        fun of(value: String): PlayerId {
            require(value.isNotBlank()) { "PlayerId value must not be blank" }
            return PlayerId(value)
        }
    }
}
