package com.julian.dixmille.core.data.mapper

import com.julian.dixmille.core.data.db.PlayerEntity
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedPlayerMapperTest {

    @Test
    fun `should map all fields when converting entity to domain`() {
        // Arrange
        val entity = PlayerEntity("id-1", "Alice", 1000L, 2000L)

        // Act
        val result = entity.toDomain()

        // Assert
        assertEquals(SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, 2000L), result)
    }

    @Test
    fun `should preserve null lastPlayedAt when converting entity to domain`() {
        // Arrange
        val entity = PlayerEntity("id-1", "Alice", 1000L, null)

        // Act
        val result = entity.toDomain()

        // Assert
        assertNull(result.lastPlayedAt)
    }

    @Test
    fun `should map all fields when converting domain to entity`() {
        // Arrange
        val player = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, 2000L)

        // Act
        val result = player.toEntity()

        // Assert
        assertEquals(PlayerEntity("id-1", "Alice", 1000L, 2000L), result)
    }

    @Test
    fun `should preserve null lastPlayedAt when converting domain to entity`() {
        // Arrange
        val player = SavedPlayer(PlayerId("id-1"), PlayerName("Alice"), 1000L, null)

        // Act
        val result = player.toEntity()

        // Assert
        assertNull(result.lastPlayedAt)
    }

    @Test
    fun `should produce equal entity after round-trip mapping`() {
        // Arrange
        val entity = PlayerEntity("id-1", "Alice", 1000L, 2000L)

        // Act
        val result = entity.toDomain().toEntity()

        // Assert
        assertEquals(entity, result)
    }
}
