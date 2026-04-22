package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class EntryMinimumScore(val value: Int) {

    init {
        require(value >= 0) { "EntryMinimumScore must be >= 0, was $value" }
    }

    override fun toString(): String = value.toString()

    companion object {
        val DEFAULT: EntryMinimumScore = EntryMinimumScore(500)
        val ZERO: EntryMinimumScore = EntryMinimumScore(0)
    }
}
