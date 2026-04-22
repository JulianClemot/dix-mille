package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class PlayerName(val value: String) {

    init {
        require(value.isNotBlank()) { "PlayerName must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "PlayerName must not exceed $MAX_LENGTH characters, was ${value.length}"
        }
    }

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH = 30
    }
}
