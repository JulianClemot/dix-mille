package com.julian.dixmille.core.domain.model.vo

value class EntryId private constructor(val value: String) {

    companion object {
        fun of(value: String): EntryId {
            require(value.isNotBlank()) { "EntryId value must not be blank" }
            return EntryId(value)
        }
    }
}
