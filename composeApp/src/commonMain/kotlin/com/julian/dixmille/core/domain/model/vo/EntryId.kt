package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class EntryId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        fun of(value: String): EntryId {
            require(value.isNotBlank()) { "EntryId value must not be blank" }
            return EntryId(value)
        }
    }
}
