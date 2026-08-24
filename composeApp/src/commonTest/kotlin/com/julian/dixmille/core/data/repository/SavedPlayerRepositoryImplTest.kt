package com.julian.dixmille.core.data.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.julian.dixmille.core.data.db.AppDatabase
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SavedPlayerRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SavedPlayerRepositoryImpl

    @BeforeTest
    fun setup() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = SavedPlayerRepositoryImpl(db.playerDao())
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `should return empty list when no players exist`() = runTest {
        // Act
        val result = repository.getAllPlayers()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `should return mapped SavedPlayer when player exists in database`() = runTest {
        // Arrange
        val player = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        repository.addPlayer(player)

        // Act
        val result = repository.getAllPlayers()

        // Assert
        assertEquals(1, result.size)
        assertEquals(PlayerName("Alice"), result.first().name)
    }

    @Test
    fun `should return success and persist player when name is unique`() = runTest {
        // Arrange
        val player = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)

        // Act
        val result = repository.addPlayer(player)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, repository.getAllPlayers().size)
    }

    @Test
    fun `should return failure when player name already exists`() = runTest {
        // Arrange
        val alice = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        repository.addPlayer(alice)

        // Act
        val secondResult = repository.addPlayer(
            SavedPlayer(PlayerId("id-2"), PlayerName("Alice"), 2000L, null),
        )

        // Assert
        assertTrue(secondResult.isFailure)
        assertIs<IllegalArgumentException>(secondResult.exceptionOrNull())
        assertEquals(1, repository.getAllPlayers().size)
    }

    @Test
    fun `should return failure when player name exists with different case`() = runTest {
        // Arrange
        val alice = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        repository.addPlayer(alice)

        // Act
        val result = repository.addPlayer(
            SavedPlayer(PlayerId("id-2"), PlayerName("alice"), 2000L, null),
        )

        // Assert
        assertTrue(result.isFailure)
        assertEquals(1, repository.getAllPlayers().size)
    }

    @Test
    fun `should update lastPlayedAt when called with valid playerId`() = runTest {
        // Arrange
        val player = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        repository.addPlayer(player)

        // Act
        repository.updateLastPlayedAt("id-1", 9999L)

        // Assert
        assertEquals(9999L, repository.getAllPlayers().first().lastPlayedAt)
    }

    @Test
    fun `should return true when name exists case-insensitively`() = runTest {
        // Arrange
        repository.addPlayer(SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null))

        // Act
        val result = repository.playerExistsByNameIgnoreCase("ALICE")

        // Assert
        assertTrue(result)
    }

    @Test
    fun `should return false when name does not exist`() = runTest {
        // Arrange
        repository.addPlayer(SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null))

        // Act
        val result = repository.playerExistsByNameIgnoreCase("Bob")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `should return success and remove player when player exists`() = runTest {
        // Arrange
        val alice = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        repository.addPlayer(alice)

        // Act
        val result = repository.deletePlayer("id-1")

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(repository.getAllPlayers().any { it.id == PlayerId("id-1") })
    }

    @Test
    fun `should return success when deleting a player that does not exist`() = runTest {
        // Act
        val result = repository.deletePlayer("ghost-id")

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should leave other players untouched when deleting one player`() = runTest {
        // Arrange
        val alice = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)
        val bob = SavedPlayer(PlayerId("id-2"), PlayerName("Bob"), 2000L, null)
        repository.addPlayer(alice)
        repository.addPlayer(bob)

        // Act
        repository.deletePlayer("id-1")

        // Assert
        val remaining = repository.getAllPlayers()
        assertEquals(1, remaining.size)
        assertEquals(PlayerName("Bob"), remaining.first().name)
    }

    @Test
    fun `should remove play history when deleting a player`() = runTest {
        // Arrange
        val alice = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 9999L, null)
        repository.addPlayer(alice)

        // Act
        repository.deletePlayer("id-1")

        // Assert
        assertFalse(repository.getAllPlayers().any { it.id == PlayerId("id-1") })
    }
}
