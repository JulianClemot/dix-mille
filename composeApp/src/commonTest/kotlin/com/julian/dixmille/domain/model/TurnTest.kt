package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnTest {

    @Test
    fun `Turn total should return zero when no entries`() {
        // Arrange
        val turn = Turn(id = TurnId("turn1"))

        // Act & Assert
        assertEquals(Score.ZERO, turn.turnTotal)
    }

    @Test
    fun `Turn total should sum points when entries exist`() {
        // Arrange
        val turn = Turn(
            id = TurnId("turn1"),
            entries = listOf(
                ScoreEntry(id = EntryId("e1"), points = Score(100)),
                ScoreEntry(id = EntryId("e2"), points = Score(200)),
                ScoreEntry(id = EntryId("e3"), points = Score(50))
            )
        )

        // Act & Assert
        assertEquals(Score(350), turn.turnTotal)
    }

    @Test
    fun `Turn total should return zero when busted`() {
        // Arrange
        val turn = Turn(
            id = TurnId("turn1"),
            entries = listOf(
                ScoreEntry(id = EntryId("e1"), points = Score(100)),
                ScoreEntry(id = EntryId("e2"), points = Score(200))
            ),
            isBusted = true
        )

        // Act & Assert
        assertEquals(Score.ZERO, turn.turnTotal)
    }

    @Test
    fun `Add entry should add entry to list`() {
        // Arrange
        val turn = Turn(id = TurnId("turn1"))
        val entry = ScoreEntry(id = EntryId("e1"), points = Score(100))

        // Act
        val updated = turn.addEntry(entry)

        // Assert
        assertEquals(1, updated.entries.size)
        assertEquals(Score(100), updated.entries.first().points)
    }

    @Test
    fun `Remove last entry should remove last when entries exist`() {
        // Arrange
        val turn = Turn(
            id = TurnId("turn1"),
            entries = listOf(
                ScoreEntry(id = EntryId("e1"), points = Score(100)),
                ScoreEntry(id = EntryId("e2"), points = Score(200))
            )
        )

        // Act
        val updated = turn.removeLastEntry()

        // Assert
        assertEquals(1, updated.entries.size)
        assertEquals(Score(100), updated.entries.first().points)
    }

    @Test
    fun `Remove last entry should return same turn when no entries`() {
        // Arrange
        val turn = Turn(id = TurnId("turn1"))

        // Act
        val updated = turn.removeLastEntry()

        // Assert
        assertEquals(0, updated.entries.size)
    }

    @Test
    fun `Bust should mark turn as busted`() {
        // Arrange
        val turn = Turn(id = TurnId("turn1"))

        // Act
        val busted = turn.bust()

        // Assert
        assertTrue(busted.isBusted)
    }
}
