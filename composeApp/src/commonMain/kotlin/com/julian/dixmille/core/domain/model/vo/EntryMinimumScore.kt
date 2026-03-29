package com.julian.dixmille.core.domain.model.vo

import kotlinx.serialization.Serializable

@Serializable
value class EntryMinimumScore private constructor(val value: Int) {

    companion object {
        val DEFAULT: EntryMinimumScore = EntryMinimumScore(500)

        fun of(value: Int): EntryMinimumScore {
            require(value > 0) { "EntryMinimumScore must be > 0, was $value" }
            return EntryMinimumScore(value)
        }
    }
}
