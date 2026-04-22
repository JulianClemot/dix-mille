package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class EntryMinimumScore private constructor(val value: Int) {

    companion object {
        val DEFAULT: EntryMinimumScore = EntryMinimumScore(500)
        val ZERO: EntryMinimumScore = EntryMinimumScore(0)

        fun of(value: Int): EntryMinimumScore {
            require(value > 0) { "EntryMinimumScore must be > 0, was $value" }
            return EntryMinimumScore(value)
        }
    }
}
