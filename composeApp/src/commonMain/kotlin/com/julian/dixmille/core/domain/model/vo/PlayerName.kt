package com.julian.dixmille.core.domain.model.vo

value class PlayerName private constructor(val value: String) {

    companion object {
        private const val MAX_LENGTH = 30

        fun of(value: String): PlayerName {
            val trimmed = value.trim()
            require(trimmed.isNotBlank()) { "PlayerName must not be blank" }
            require(trimmed.length <= MAX_LENGTH) {
                "PlayerName must not exceed $MAX_LENGTH characters, was ${trimmed.length}"
            }
            return PlayerName(trimmed)
        }
    }
}
