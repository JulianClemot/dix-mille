package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreEntryVoTest {

    @Test
    fun `Should create ScoreEntry with EntryId and Score`() {
        // Arrange
        val id = EntryId("entry-1")
        val points = Score(100)

        // Act
        val entry = ScoreEntry(id = id, points = points)

        // Assert
        assertEquals(id, entry.id)
        assertEquals(points, entry.points)
    }

    @Test
    fun `Should compute turn total as Score`() {
        // Arrange
        val turn = Turn(
            id = TurnId("turn-1"),
            entries = listOf(
                ScoreEntry(id = EntryId("e1"), points = Score(100)),
                ScoreEntry(id = EntryId("e2"), points = Score(200))
            )
        )

        // Act
        val total = turn.turnTotal

        // Assert
        assertEquals(Score(300), total)
    }
}
