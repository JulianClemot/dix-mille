package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class PlayerId(val value: String) {

    init {
        require(value.isNotBlank()) { "PlayerId value must not be blank" }
    }

    override fun toString(): String = value
}
