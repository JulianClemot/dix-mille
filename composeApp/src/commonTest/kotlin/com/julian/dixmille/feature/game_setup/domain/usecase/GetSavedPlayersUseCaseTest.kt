package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSavedPlayersUseCaseTest {

    private val repository = FakeSavedPlayerRepository()
    private val useCase = GetSavedPlayersUseCase(repository)

    private fun savedPlayer(id: String, name: String): SavedPlayer = SavedPlayer(
        id = PlayerId(id),
        name = PlayerName(name),
        createdAt = 0L,
        lastPlayedAt = null,
    )

    @Test
    fun `should return empty list when no players exist`() = runTest {
        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return all players when players exist`() = runTest {
        repository.players.add(savedPlayer("1", "Alice"))
        repository.players.add(savedPlayer("2", "Bob"))

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `should return players in alphabetical order regardless of repository order`() = runTest {
        repository.players.add(savedPlayer("1", "Zara"))
        repository.players.add(savedPlayer("2", "Alice"))
        repository.players.add(savedPlayer("3", "Bob"))

        val result = useCase()

        assertEquals(listOf("Alice", "Bob", "Zara"), result.map { it.name.value })
    }

    @Test
    fun `should sort alphabetically case-insensitively`() = runTest {
        repository.players.add(savedPlayer("1", "bob"))
        repository.players.add(savedPlayer("2", "Alice"))

        val result = useCase()

        assertEquals(listOf("Alice", "bob"), result.map { it.name.value })
    }

    @Test
    fun `should return single player when only one player exists`() = runTest {
        repository.players.add(savedPlayer("1", "Alice"))

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals("Alice", result[0].name.value)
    }
}
