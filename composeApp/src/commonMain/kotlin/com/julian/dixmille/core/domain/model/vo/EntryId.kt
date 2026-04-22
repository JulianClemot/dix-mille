package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class EntryId(val value: String) {

    init {
        require(value.isNotBlank()) { "EntryId value must not be blank" }
    }

    override fun toString(): String = value
}
