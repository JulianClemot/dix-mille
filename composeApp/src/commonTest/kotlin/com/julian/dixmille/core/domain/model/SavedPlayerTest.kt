package com.julian.dixmille.core.domain.model

import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SavedPlayerTest {

    @Test
    fun `should create SavedPlayer when all fields are valid`() {
        // Arrange
        val id = PlayerId("player-uuid-1")
        val name = PlayerName("Alice")
        val createdAt = 1000L
        val lastPlayedAt = 2000L

        // Act
        val player = SavedPlayer(
            id = id,
            name = name,
            createdAt = createdAt,
            lastPlayedAt = lastPlayedAt,
        )

        // Assert
        assertEquals(
            SavedPlayer(
                id = PlayerId("player-uuid-1"),
                name = PlayerName("Alice"),
                createdAt = 1000L,
                lastPlayedAt = 2000L,
            ),
            player,
        )
    }

    @Test
    fun `should not be equal when lastPlayedAt differs`() {
        // Arrange
        val player1 = SavedPlayer(
            id = PlayerId("player-uuid-10"),
            name = PlayerName("Eve"),
            createdAt = 5000L,
            lastPlayedAt = 6000L,
        )
        val player2 = SavedPlayer(
            id = PlayerId("player-uuid-10"),
            name = PlayerName("Eve"),
            createdAt = 5000L,
            lastPlayedAt = null,
        )

        // Assert
        assertNotEquals(player1, player2)
    }

    @Test
    fun `should not be equal when ids differ`() {
        // Arrange
        val player1 = SavedPlayer(
            id = PlayerId("player-uuid-9a"),
            name = PlayerName("Dave"),
            createdAt = 5000L,
            lastPlayedAt = 6000L,
        )
        val player2 = SavedPlayer(
            id = PlayerId("player-uuid-9b"),
            name = PlayerName("Dave"),
            createdAt = 5000L,
            lastPlayedAt = 6000L,
        )

        // Assert
        assertNotEquals(player1, player2)
    }

    @Test
    fun `should be equal when all fields match`() {
        // Arrange
        val player1 = SavedPlayer(
            id = PlayerId("player-uuid-8"),
            name = PlayerName("Carol"),
            createdAt = 5000L,
            lastPlayedAt = 6000L,
        )
        val player2 = SavedPlayer(
            id = PlayerId("player-uuid-8"),
            name = PlayerName("Carol"),
            createdAt = 5000L,
            lastPlayedAt = 6000L,
        )

        // Assert
        assertEquals(player1, player2)
    }

    @Test
    fun `should create SavedPlayer when name is one character`() {
        // Arrange + Act
        val player = SavedPlayer(
            id = PlayerId("player-uuid-7"),
            name = PlayerName("X"),
            createdAt = 1000L,
            lastPlayedAt = null,
        )

        // Assert
        assertEquals(
            SavedPlayer(
                id = PlayerId("player-uuid-7"),
                name = PlayerName("X"),
                createdAt = 1000L,
                lastPlayedAt = null,
            ),
            player,
        )
    }

    @Test
    fun `should throw when name exceeds 30 characters`() {
        // Arrange
        val thirtyOneChars = "A".repeat(31)

        // Act + Assert
        assertFailsWith<IllegalArgumentException> {
            SavedPlayer(
                id = PlayerId("player-uuid-6"),
                name = PlayerName(thirtyOneChars),
                createdAt = 1000L,
                lastPlayedAt = null,
            )
        }
    }

    @Test
    fun `should create SavedPlayer when name is exactly 30 characters`() {
        // Arrange
        val thirtyChars = "A".repeat(30)

        // Act
        val player = SavedPlayer(
            id = PlayerId("player-uuid-5"),
            name = PlayerName(thirtyChars),
            createdAt = 1000L,
            lastPlayedAt = null,
        )

        // Assert
        assertEquals(
            SavedPlayer(
                id = PlayerId("player-uuid-5"),
                name = PlayerName(thirtyChars),
                createdAt = 1000L,
                lastPlayedAt = null,
            ),
            player,
        )
    }

    @Test
    fun `should throw when name is whitespace only`() {
        // Arrange + Act + Assert
        assertFailsWith<IllegalArgumentException> {
            SavedPlayer(
                id = PlayerId("player-uuid-4"),
                name = PlayerName("   "),
                createdAt = 1000L,
                lastPlayedAt = null,
            )
        }
    }

    @Test
    fun `should throw when name is blank`() {
        // Arrange + Act + Assert
        assertFailsWith<IllegalArgumentException> {
            SavedPlayer(
                id = PlayerId("player-uuid-3"),
                name = PlayerName(""),
                createdAt = 1000L,
                lastPlayedAt = null,
            )
        }
    }

    @Test
    fun `should create SavedPlayer when lastPlayedAt is null`() {
        // Arrange + Act
        val player = SavedPlayer(
            id = PlayerId("player-uuid-2"),
            name = PlayerName("Bob"),
            createdAt = 1000L,
            lastPlayedAt = null,
        )

        // Assert
        assertEquals(
            SavedPlayer(
                id = PlayerId("player-uuid-2"),
                name = PlayerName("Bob"),
                createdAt = 1000L,
                lastPlayedAt = null,
            ),
            player,
        )
    }
}
