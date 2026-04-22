package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class TurnId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        fun of(value: String): TurnId {
            require(value.isNotBlank()) { "TurnId value must not be blank" }
            return TurnId(value)
        }
    }
}
