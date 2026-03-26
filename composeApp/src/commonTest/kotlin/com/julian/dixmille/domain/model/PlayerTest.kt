package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    @Test
    fun `Start turn should create new turn`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")

        // Act
        val updated = player.startTurn("turn1")

        // Assert
        assertNotNull(updated.currentTurn)
        assertEquals("turn1", updated.currentTurn?.id)
    }

    @Test
    fun `Add score entry should add entry to current turn`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
        val entry = ScoreEntry(id = "e1", points = 100)

        // Act
        val updated = player.addScoreEntry(entry)

        // Assert
        assertEquals(1, updated.currentTurn?.entries?.size)
        assertEquals(100, updated.currentTurn?.turnTotal)
    }

    @Test
    fun `Undo last entry should remove last entry`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 100))
            .addScoreEntry(ScoreEntry(id = "e2", points = 200))

        // Act
        val updated = player.undoLastEntry()

        // Assert
        assertEquals(1, updated.currentTurn?.entries?.size)
        assertEquals(100, updated.currentTurn?.turnTotal)
    }

    @Test
    fun `Bust turn should clear current turn`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 100))

        // Act
        val updated = player.bustTurn()

        // Assert
        assertNull(updated.currentTurn)
        assertEquals(0, updated.totalScore)
    }

    @Test
    fun `Commit turn should not add points when not entered and score is below 500`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 200))

        // Act
        val updated = player.commitTurn()

        // Assert
        assertEquals(0, updated.totalScore)
        assertFalse(updated.hasEnteredGame)
        assertNull(updated.currentTurn)
    }

    @Test
    fun `Commit turn should enter game and add points when not entered and score is 500 or more`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 500))

        // Act
        val updated = player.commitTurn()

        // Assert
        assertEquals(500, updated.totalScore)
        assertTrue(updated.hasEnteredGame)
        assertNull(updated.currentTurn)
    }

    @Test
    fun `Commit turn should add points regardless of amount when already entered`() {
        // Arrange
        val player = Player(
            id = "p1",
            name = "Alice",
            totalScore = 1000,
            hasEnteredGame = true
        )
            .startTurn("turn1")
            .addScoreEntry(ScoreEntry(id = "e1", points = 50))

        // Act
        val updated = player.commitTurn()

        // Assert
        assertEquals(1050, updated.totalScore)
        assertTrue(updated.hasEnteredGame)
        assertNull(updated.currentTurn)
    }

    @Test
    fun `Mark final round played should set flag`() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")

        // Act
        val updated = player.markFinalRoundPlayed()

        // Assert
        assertTrue(updated.hasPlayedFinalRound)
    }
}
