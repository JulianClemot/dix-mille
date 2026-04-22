package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class TurnId(val value: String) {

    init {
        require(value.isNotBlank()) { "TurnId value must not be blank" }
    }

    override fun toString(): String = value
}
