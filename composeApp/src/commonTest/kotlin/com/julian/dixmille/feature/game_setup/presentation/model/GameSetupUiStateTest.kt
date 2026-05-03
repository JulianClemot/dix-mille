package com.julian.dixmille.feature.game_setup.presentation.model

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSetupUiStateTest {

    private fun savedPlayer(id: String, name: String): SavedPlayer = SavedPlayer(
        id = PlayerId(id),
        name = PlayerName(name),
        createdAt = 0L,
        lastPlayedAt = null,
    )

    @Test
    fun `should have canStartGame false when fewer than 2 players selected`() {
        val state = GameSetupUiState(selectedPlayers = listOf(savedPlayer("1", "Alice")))

        assertFalse(state.canStartGame)
    }

    @Test
    fun `should have canStartGame true when 2 or more players selected`() {
        val state = GameSetupUiState(
            selectedPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            )
        )

        assertTrue(state.canStartGame)
    }

    @Test
    fun `should have canAddMorePlayers false when 6 players selected`() {
        val players = (1..6).map { savedPlayer("$it", "Player$it") }
        val state = GameSetupUiState(selectedPlayers = players)

        assertFalse(state.canAddMorePlayers)
    }

    @Test
    fun `should have canAddMorePlayers true when fewer than 6 players selected`() {
        val players = (1..5).map { savedPlayer("$it", "Player$it") }
        val state = GameSetupUiState(selectedPlayers = players)

        assertTrue(state.canAddMorePlayers)
    }

    // --- unifiedInput tests ---

    @Test
    fun `should initialize unifiedInput as empty string by default`() {
        val state = GameSetupUiState()

        assertEquals("", state.unifiedInput)
    }

    @Test
    fun `should return all players when unifiedInput is empty`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "",
        )

        assertEquals(2, state.filteredPlayers.size)
    }

    @Test
    fun `should return all players when unifiedInput is blank`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "   ",
        )

        assertEquals(2, state.filteredPlayers.size)
    }

    @Test
    fun `should return matching players when unifiedInput is a partial match`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "ali",
        )

        assertEquals(listOf("Alice"), state.filteredPlayers.map { it.name.value })
    }

    @Test
    fun `should filter players case-insensitively`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "ALICE",
        )

        assertEquals(listOf("alice"), state.filteredPlayers.map { it.name.value })
    }

    @Test
    fun `should filter players using contains matching`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Robert"),
            ),
            unifiedInput = "ob",
        )

        assertEquals(listOf("Robert"), state.filteredPlayers.map { it.name.value })
    }

    @Test
    fun `should return empty list when unifiedInput matches no player`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "xyz",
        )

        assertEquals(emptyList(), state.filteredPlayers)
    }

    @Test
    fun `should filter correctly with a single character input`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            unifiedInput = "b",
        )

        assertEquals(listOf("Bob"), state.filteredPlayers.map { it.name.value })
    }

    // --- canAddNewPlayer tests ---

    @Test
    fun `should have canAddNewPlayer false when unifiedInput is empty`() {
        val state = GameSetupUiState(unifiedInput = "")

        assertFalse(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer false when unifiedInput is blank`() {
        val state = GameSetupUiState(unifiedInput = "   ")

        assertFalse(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer true when unifiedInput is a new unique name`() {
        val state = GameSetupUiState(
            allPlayers = listOf(savedPlayer("1", "Alice")),
            unifiedInput = "Bob",
        )

        assertTrue(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer false when unifiedInput exactly matches an existing player name case-insensitively`() {
        val state = GameSetupUiState(
            allPlayers = listOf(savedPlayer("1", "Alice")),
            unifiedInput = "alice",
        )

        assertFalse(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer true when unifiedInput is a partial match of an existing player name`() {
        val state = GameSetupUiState(
            allPlayers = listOf(savedPlayer("1", "Alice")),
            unifiedInput = "Ali",
        )

        assertTrue(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer true when unifiedInput is a superset of an existing player name`() {
        val state = GameSetupUiState(
            allPlayers = listOf(savedPlayer("1", "Alice")),
            unifiedInput = "AliceWonder",
        )

        assertTrue(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer false when max players are already selected`() {
        val players = (1..6).map { savedPlayer("$it", "Player$it") }
        val state = GameSetupUiState(
            selectedPlayers = players,
            unifiedInput = "NewPlayer",
        )

        assertFalse(state.canAddNewPlayer)
    }

    @Test
    fun `should have canAddNewPlayer false when unifiedInput matches a player in allPlayers even if not selected`() {
        val alice = savedPlayer("1", "Alice")
        val state = GameSetupUiState(
            allPlayers = listOf(alice),
            selectedPlayers = emptyList(),
            unifiedInput = "Alice",
        )

        assertFalse(state.canAddNewPlayer)
    }
}
