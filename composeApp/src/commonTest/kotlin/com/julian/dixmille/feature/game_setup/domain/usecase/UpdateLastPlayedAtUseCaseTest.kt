package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateLastPlayedAtUseCaseTest {

    private val repository = FakeSavedPlayerRepository()
    private val useCase = UpdateLastPlayedAtUseCase(
        repository = repository,
        clock = { 9999L },
    )

    private fun savedPlayer(id: String, name: String): SavedPlayer = SavedPlayer(
        id = PlayerId(id),
        name = PlayerName(name),
        createdAt = 0L,
        lastPlayedAt = null,
    )

    @Test
    fun `should update lastPlayedAt for all provided player ids`() = runTest {
        repository.players.add(savedPlayer("id-1", "Alice"))
        repository.players.add(savedPlayer("id-2", "Bob"))

        useCase(listOf("id-1", "id-2"))

        assertEquals(9999L, repository.players[0].lastPlayedAt)
        assertEquals(9999L, repository.players[1].lastPlayedAt)
    }

    @Test
    fun `should use the current timestamp for all updates`() = runTest {
        repository.players.add(savedPlayer("id-1", "Alice"))
        repository.players.add(savedPlayer("id-2", "Bob"))

        useCase(listOf("id-1", "id-2"))

        assertEquals(9999L, repository.players.find { it.id.value == "id-1" }?.lastPlayedAt)
        assertEquals(9999L, repository.players.find { it.id.value == "id-2" }?.lastPlayedAt)
    }

    @Test
    fun `should do nothing when player ids list is empty`() = runTest {
        repository.players.add(savedPlayer("id-1", "Alice"))

        useCase(emptyList())

        assertNull(repository.players[0].lastPlayedAt)
    }
}
