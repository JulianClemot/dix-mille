package com.julian.dixmille.core.domain.model.vo

import kotlin.jvm.JvmInline

@JvmInline
value class TargetScore(val value: Int) {

    init {
        require(value >= 1000) { "TargetScore must be >= 1000, was $value" }
    }

    override fun toString(): String = value.toString()

    companion object {
        val DEFAULT: TargetScore = TargetScore(10_000)
    }
}
