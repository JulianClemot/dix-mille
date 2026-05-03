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

    @Test
    fun `should filter players by search query case-insensitively`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            searchQuery = "ali",
        )

        assertEquals(listOf("Alice"), state.filteredPlayers.map { it.name.value })
    }

    @Test
    fun `should return all players when search query is blank`() {
        val state = GameSetupUiState(
            allPlayers = listOf(
                savedPlayer("1", "Alice"),
                savedPlayer("2", "Bob"),
            ),
            searchQuery = "",
        )

        assertEquals(2, state.filteredPlayers.size)
    }
}
