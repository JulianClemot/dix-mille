package com.julian.dixmille.data.repository

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.Player
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameRepositoryImplTest {
    
    private lateinit var fakeStorage: FakeLocalStorage
    private lateinit var repository: GameRepositoryImpl
    
    private fun setup() {
        fakeStorage = FakeLocalStorage()
        repository = GameRepositoryImpl(fakeStorage)
    }
    
    @Test
    fun saveGame_shouldPersistGameAsJson() = runTest {
        // Arrange
        setup()
        val game = createTestGame()
        
        // Act
        val result = repository.saveGame(game)
        
        // Assert
        assertTrue(result.isSuccess)
        val storedJson = fakeStorage.getString("current_game")
        assertNotNull(storedJson)
        assertTrue(storedJson.contains("game1"))
        assertTrue(storedJson.contains("Alice"))
    }
    
    @Test
    fun getCurrentGame_whenGameExists_shouldReturnGame() = runTest {
        // Arrange
        setup()
        val game = createTestGame()
        repository.saveGame(game)
        
        // Act
        val result = repository.getCurrentGame()
        
        // Assert
        assertTrue(result.isSuccess)
        val retrievedGame = result.getOrNull()
        assertNotNull(retrievedGame)
        assertEquals(game.id, retrievedGame.id)
        assertEquals(game.players.size, retrievedGame.players.size)
        assertEquals(game.players[0].name, retrievedGame.players[0].name)
        assertEquals(game.targetScore, retrievedGame.targetScore)
    }
    
    @Test
    fun getCurrentGame_whenNoGame_shouldReturnFailure() = runTest {
        // Arrange
        setup()
        
        // Act
        val result = repository.getCurrentGame()
        
        // Assert
        assertTrue(result.isFailure)
    }
    
    @Test
    fun deleteGame_shouldRemoveGame() = runTest {
        // Arrange
        setup()
        val game = createTestGame()
        repository.saveGame(game)
        
        // Act
        val deleteResult = repository.deleteGame()
        
        // Assert
        assertTrue(deleteResult.isSuccess)
        val getResult = repository.getCurrentGame()
        assertTrue(getResult.isFailure)
    }
    
    @Test
    fun hasGame_whenGameExists_shouldReturnTrue() = runTest {
        // Arrange
        setup()
        val game = createTestGame()
        repository.saveGame(game)
        
        // Act
        val hasGame = repository.hasGame()
        
        // Assert
        assertTrue(hasGame)
    }
    
    @Test
    fun hasGame_whenNoGame_shouldReturnFalse() = runTest {
        // Arrange
        setup()
        
        // Act
        val hasGame = repository.hasGame()
        
        // Assert
        assertFalse(hasGame)
    }
    
    @Test
    fun saveAndRetrieve_shouldPreserveAllGameState() = runTest {
        // Arrange
        setup()
        val player1 = Player(
            id = "p1",
            name = "Alice",
            totalScore = 1500,
            hasEnteredGame = true
        )
        val player2 = Player(
            id = "p2",
            name = "Bob",
            totalScore = 800,
            hasEnteredGame = true
        )
        val game = Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            createdAt = 123456789L
        )
        
        // Act
        repository.saveGame(game)
        val result = repository.getCurrentGame()
        
        // Assert
        assertTrue(result.isSuccess)
        val retrieved = result.getOrNull()!!
        assertEquals(game.id, retrieved.id)
        assertEquals(game.players.size, retrieved.players.size)
        assertEquals(game.players[0].totalScore, retrieved.players[0].totalScore)
        assertEquals(game.players[0].hasEnteredGame, retrieved.players[0].hasEnteredGame)
        assertEquals(game.currentPlayerIndex, retrieved.currentPlayerIndex)
        assertEquals(game.gamePhase, retrieved.gamePhase)
        assertEquals(game.targetScore, retrieved.targetScore)
        assertEquals(game.createdAt, retrieved.createdAt)
    }
    
    private fun createTestGame(): Game {
        val player1 = Player(id = "p1", name = "Alice")
        val player2 = Player(id = "p2", name = "Bob")
        
        return Game(
            id = "game1",
            players = listOf(player1, player2),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            createdAt = 0L
        )
    }
}
