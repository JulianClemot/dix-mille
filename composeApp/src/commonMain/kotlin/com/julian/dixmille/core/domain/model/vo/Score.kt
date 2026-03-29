package com.julian.dixmille.core.domain.model.vo

value class Score private constructor(val value: Int) : Comparable<Score> {

    override fun compareTo(other: Score): Int = value.compareTo(other.value)

    operator fun plus(other: Score): Score = Score(value + other.value)

    fun meetsEntryThreshold(): Boolean = value >= 500

    companion object {
        val ZERO: Score = Score(0)

        fun of(value: Int): Score {
            require(value >= 0) { "Score value must be >= 0, was $value" }
            require(value % 50 == 0) { "Score value must be a multiple of 50, was $value" }
            return Score(value)
        }
    }
}
