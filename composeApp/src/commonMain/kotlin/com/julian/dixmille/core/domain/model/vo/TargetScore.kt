package com.julian.dixmille.core.domain.model.vo

value class TargetScore private constructor(val value: Int) {

    companion object {
        val DEFAULT: TargetScore = TargetScore(10_000)

        fun of(value: Int): TargetScore {
            require(value >= 1000) { "TargetScore must be >= 1000, was $value" }
            return TargetScore(value)
        }
    }
}
