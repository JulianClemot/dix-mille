package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class GameId(val value: String) {

    init {
        require(value.isNotBlank()) { "GameId value must not be blank" }
    }

    override fun toString(): String = value
}
