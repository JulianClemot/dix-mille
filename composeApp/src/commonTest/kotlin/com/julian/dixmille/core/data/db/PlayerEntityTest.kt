package com.julian.dixmille.core.data.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PlayerEntityTest {

    @Test
    fun `should create PlayerEntity when all fields are valid`() {
        // Arrange + Act
        val entity = PlayerEntity(
            id = "id-1",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = 2000L,
        )

        // Assert
        assertEquals(
            PlayerEntity(
                id = "id-1",
                name = "Alice",
                createdAt = 1000L,
                lastPlayedAt = 2000L,
            ),
            entity,
        )
    }

    @Test
    fun `should create PlayerEntity when lastPlayedAt is null`() {
        // Arrange + Act
        val entity = PlayerEntity(
            id = "id-1",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = null,
        )

        // Assert
        assertNull(entity.lastPlayedAt)
    }

    @Test
    fun `should be equal when all fields match`() {
        // Arrange
        val a = PlayerEntity(
            id = "id-1",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = 2000L,
        )
        val b = PlayerEntity(
            id = "id-1",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = 2000L,
        )

        // Assert
        assertEquals(a, b)
    }

    @Test
    fun `should not be equal when ids differ`() {
        // Arrange
        val a = PlayerEntity(
            id = "id-1",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = 2000L,
        )
        val b = PlayerEntity(
            id = "id-2",
            name = "Alice",
            createdAt = 1000L,
            lastPlayedAt = 2000L,
        )

        // Assert
        assertNotEquals(a, b)
    }
}
