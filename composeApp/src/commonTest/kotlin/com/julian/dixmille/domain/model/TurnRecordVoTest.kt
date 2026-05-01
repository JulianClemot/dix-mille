package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.Score
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnRecordVoTest {

    @Test
    fun `Should create TurnRecord with PlayerId Score and previousScore as Score`() {
        // Arrange
        val playerId = PlayerId("player-1")
        val points = Score(500)
        val previousScore = Score(1000)

        // Act
        val record = TurnRecord(
            roundNumber = 1,
            playerId = playerId,
            points = points,
            outcome = TurnOutcome.SCORED,
            previousScore = previousScore
        )

        // Assert
        val expected = TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("player-1"),
            points = Score(500),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(1000)
        )
        assertEquals(expected, record)
    }
}
