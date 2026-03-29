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
        val playerId = PlayerId.of("player-1")
        val points = Score.of(500)
        val previousScore = Score.of(1000)

        // Act
        val record = TurnRecord(
            roundNumber = 1,
            playerId = playerId,
            points = points,
            outcome = TurnOutcome.SCORED,
            previousScore = previousScore
        )

        // Assert
        assertEquals(playerId, record.playerId)
        assertEquals(points, record.points)
        assertEquals(previousScore, record.previousScore)
        assertEquals(1, record.roundNumber)
    }
}
