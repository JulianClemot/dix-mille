package com.julian.dixmille.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {
    
    @Test
    fun startTurn_shouldCreateNewTurn() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
        
        // Act
        val updated = player.startTurn("turn1")
        
        // Assert
        assertNotNull(updated.currentTurn)
        assertEquals("turn1", updated.currentTurn?.id)
    }
    
    @Test
    fun addScoreEntry_shouldAddEntryToCurrentTurn() {
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
    fun undoLastEntry_shouldRemoveLastEntry() {
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
    fun bustTurn_shouldClearCurrentTurn() {
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
    fun commitTurn_whenNotEntered_andScoreBelow500_shouldNotAddPoints() {
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
    fun commitTurn_whenNotEntered_andScore500OrMore_shouldEnterAndAddPoints() {
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
    fun commitTurn_whenAlreadyEntered_shouldAddPointsRegardlessOfAmount() {
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
    fun markFinalRoundPlayed_shouldSetFlag() {
        // Arrange
        val player = Player(id = "p1", name = "Alice")
        
        // Act
        val updated = player.markFinalRoundPlayed()
        
        // Assert
        assertTrue(updated.hasPlayedFinalRound)
    }
}
