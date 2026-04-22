package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class BustCount private constructor(val value: Int) {

    fun increment(): BustCount {
        check(value < 3) { "BustCount cannot be incremented beyond 3" }
        return BustCount(value + 1)
    }

    fun isMaxed(): Boolean = value == 3

    companion object {
        val NONE: BustCount = BustCount(0)

        fun of(value: Int): BustCount {
            require(value in 0..3) { "BustCount value must be in 0..3, was $value" }
            return BustCount(value)
        }
    }
}
