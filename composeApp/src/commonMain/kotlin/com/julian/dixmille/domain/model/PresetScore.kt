package com.julian.dixmille.domain.model

/**
 * Represents a preset score value for quick entry.
 */
data class PresetScore(
    val points: Int,
    val label: String
)

/**
 * All available preset scores based on Dix Mille rules.
 */
object PresetScores {
    val ONE_5 = PresetScore(50, "One 5")
    val ONE_1 = PresetScore(100, "One 1")
    val ONE_1_ONE_5 = PresetScore(150, "1 + 5")
    val TWO_1S_OR_THREE_2S = PresetScore(200, "Two 1s / Three 2s")
    val TWO_1S_ONE_5 = PresetScore(250, "Two 1s + 5")
    val THREE_1S_OR_THREE_3S = PresetScore(300, "Three 1s / Three 3s")
    val FOUR_1S_OR_THREE_4S = PresetScore(400, "Four 1s / Three 4s")
    val FIVE_1S_OR_THREE_5S = PresetScore(500, "Five 1s / Three 5s")
    val SIX_1S_OR_THREE_6S = PresetScore(600, "Six 1s / Three 6s")
    val THREE_1S_FIRST_ROLL = PresetScore(1000, "Three 1s (first roll)")
    val FOUR_1S_FIRST_ROLL = PresetScore(1500, "Four 1s (first roll)")
    val FIVE_1S_FIRST_ROLL = PresetScore(2000, "Five 1s (first roll)")

    /**
     * All preset scores in display order.
     */
    val all: List<PresetScore> = listOf(
        ONE_5,
        ONE_1,
        ONE_1_ONE_5,
        TWO_1S_OR_THREE_2S,
        TWO_1S_ONE_5,
        THREE_1S_OR_THREE_3S,
        FOUR_1S_OR_THREE_4S,
        FIVE_1S_OR_THREE_5S,
        SIX_1S_OR_THREE_6S,
        THREE_1S_FIRST_ROLL,
        FOUR_1S_FIRST_ROLL,
        FIVE_1S_FIRST_ROLL
    )
    
    /**
     * Quick access to preset score values.
     */
    val validPresetValues: Set<Int> = all.map { it.points }.toSet()
}
