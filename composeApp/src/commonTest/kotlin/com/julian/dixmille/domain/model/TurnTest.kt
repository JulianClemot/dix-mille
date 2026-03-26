package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnTest {

    @Test
    fun `Turn total should return zero when no entries`() {
        // Arrange
        val turn = Turn(id = "turn1")

        // Act & Assert
        assertEquals(0, turn.turnTotal)
    }

    @Test
    fun `Turn total should sum points when entries exist`() {
        // Arrange
        val turn = Turn(
            id = "turn1",
            entries = listOf(
                ScoreEntry(id = "e1", points = 100),
                ScoreEntry(id = "e2", points = 200),
                ScoreEntry(id = "e3", points = 50)
            )
        )

        // Act & Assert
        assertEquals(350, turn.turnTotal)
    }

    @Test
    fun `Turn total should return zero when busted`() {
        // Arrange
        val turn = Turn(
            id = "turn1",
            entries = listOf(
                ScoreEntry(id = "e1", points = 100),
                ScoreEntry(id = "e2", points = 200)
            ),
            isBusted = true
        )

        // Act & Assert
        assertEquals(0, turn.turnTotal)
    }

    @Test
    fun `Add entry should add entry to list`() {
        // Arrange
        val turn = Turn(id = "turn1")
        val entry = ScoreEntry(id = "e1", points = 100)

        // Act
        val updated = turn.addEntry(entry)

        // Assert
        assertEquals(1, updated.entries.size)
        assertEquals(100, updated.entries.first().points)
    }

    @Test
    fun `Remove last entry should remove last when entries exist`() {
        // Arrange
        val turn = Turn(
            id = "turn1",
            entries = listOf(
                ScoreEntry(id = "e1", points = 100),
                ScoreEntry(id = "e2", points = 200)
            )
        )

        // Act
        val updated = turn.removeLastEntry()

        // Assert
        assertEquals(1, updated.entries.size)
        assertEquals(100, updated.entries.first().points)
    }

    @Test
    fun `Remove last entry should return same turn when no entries`() {
        // Arrange
        val turn = Turn(id = "turn1")

        // Act
        val updated = turn.removeLastEntry()

        // Assert
        assertEquals(0, updated.entries.size)
    }

    @Test
    fun `Bust should mark turn as busted`() {
        // Arrange
        val turn = Turn(id = "turn1")

        // Act
        val busted = turn.bust()

        // Assert
        assertTrue(busted.isBusted)
    }
}
