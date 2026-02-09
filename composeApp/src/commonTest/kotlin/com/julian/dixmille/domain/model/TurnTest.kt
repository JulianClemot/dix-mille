package com.julian.dixmille.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnTest {
    
    @Test
    fun turnTotal_whenNoEntries_shouldReturnZero() {
        // Arrange
        val turn = Turn(id = "turn1")
        
        // Act & Assert
        assertEquals(0, turn.turnTotal)
    }
    
    @Test
    fun turnTotal_whenEntriesExist_shouldSumPoints() {
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
    fun turnTotal_whenBusted_shouldReturnZero() {
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
    fun addEntry_shouldAddEntryToList() {
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
    fun removeLastEntry_whenEntriesExist_shouldRemoveLast() {
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
    fun removeLastEntry_whenNoEntries_shouldReturnSameTurn() {
        // Arrange
        val turn = Turn(id = "turn1")
        
        // Act
        val updated = turn.removeLastEntry()
        
        // Assert
        assertEquals(0, updated.entries.size)
    }
    
    @Test
    fun bust_shouldMarkTurnAsBusted() {
        // Arrange
        val turn = Turn(id = "turn1")
        
        // Act
        val busted = turn.bust()
        
        // Assert
        assertTrue(busted.isBusted)
    }
}
