package com.julian.dixmille.core.data.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AppDatabaseTest {

    private fun buildInMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()

    @Test
    fun `should build AppDatabase successfully`() = runTest {
        // Arrange + Act
        val db = buildInMemoryDb()

        // Assert
        assertNotNull(db)
        db.close()
    }

    @Test
    fun `should return non-null PlayerDao from database`() = runTest {
        // Arrange
        val db = buildInMemoryDb()

        // Act
        val dao = db.playerDao()

        // Assert
        assertNotNull(dao)
        db.close()
    }

    @Test
    fun `should return empty list when no players exist`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()

        // Act
        val result = dao.getAllPlayers()

        // Assert
        assertEquals(emptyList(), result)
        db.close()
    }

    @Test
    fun `should return inserted player when getAllPlayers is called`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        val entity = PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null)

        // Act
        dao.insert(entity)
        val result = dao.getAllPlayers()

        // Assert
        assertEquals(listOf(entity), result)
        db.close()
    }

    @Test
    fun `should return players in alphabetical order`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        val zara = PlayerEntity(id = "id-2", name = "Zara", createdAt = 1000L, lastPlayedAt = null)
        val alice = PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null)

        // Act
        dao.insert(zara)
        dao.insert(alice)
        val result = dao.getAllPlayers()

        // Assert
        assertEquals(listOf(alice, zara), result)
        db.close()
    }

    @Test
    fun `should return true when name matches exactly`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        dao.insert(PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null))

        // Act
        val result = dao.existsByNameIgnoreCase("Alice")

        // Assert
        assertTrue(result)
        db.close()
    }

    @Test
    fun `should return true when name matches case-insensitively`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        dao.insert(PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null))

        // Act
        val result = dao.existsByNameIgnoreCase("alice")

        // Assert
        assertTrue(result)
        db.close()
    }

    @Test
    fun `should return false when name does not exist`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        dao.insert(PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null))

        // Act
        val result = dao.existsByNameIgnoreCase("Bob")

        // Assert
        assertFalse(result)
        db.close()
    }

    @Test
    fun `should update lastPlayedAt for the given player`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        dao.insert(PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null))

        // Act
        dao.updateLastPlayedAt("id-1", 9999L)
        val result = dao.getAllPlayers()

        // Assert
        assertEquals(
            PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = 9999L),
            result.first(),
        )
        db.close()
    }

    @Test
    fun `should not update other players when updating lastPlayedAt`() = runTest {
        // Arrange
        val db = buildInMemoryDb()
        val dao = db.playerDao()
        val alice = PlayerEntity(id = "id-1", name = "Alice", createdAt = 1000L, lastPlayedAt = null)
        val bob = PlayerEntity(id = "id-2", name = "Bob", createdAt = 1000L, lastPlayedAt = null)
        dao.insert(alice)
        dao.insert(bob)

        // Act
        dao.updateLastPlayedAt("id-1", 9999L)
        val result = dao.getAllPlayers()

        // Assert
        val bobResult = result.first { it.name == "Bob" }
        assertEquals(bob, bobResult)
        db.close()
    }
}
