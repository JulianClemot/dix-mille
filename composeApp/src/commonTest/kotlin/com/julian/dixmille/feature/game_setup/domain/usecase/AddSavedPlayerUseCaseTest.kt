package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddSavedPlayerUseCaseTest {

    private val repository = FakeSavedPlayerRepository()
    private val useCase = AddSavedPlayerUseCase(
        repository = repository,
        generateId = { "test-id" },
        clock = { 1000L },
    )

    @Test
    fun `should create and persist player when name is valid`() = runTest {
        val result = useCase("Alice")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.players.size)
        val player = result.getOrThrow()
        assertEquals(
            SavedPlayer(
                id = PlayerId("test-id"),
                name = PlayerName("Alice"),
                createdAt = 1000L,
                lastPlayedAt = null,
            ),
            player,
        )
    }

    @Test
    fun `should trim whitespace from name before saving`() = runTest {
        val result = useCase("  Alice  ")

        assertTrue(result.isSuccess)
        assertEquals("Alice", result.getOrThrow().name.value)
    }

    @Test
    fun `should return failure when name is blank`() = runTest {
        val result = useCase("")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should return failure when name is whitespace only`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when name already exists`() = runTest {
        repository.players.add(
            SavedPlayer(
                id = PlayerId("existing-id"),
                name = PlayerName("Alice"),
                createdAt = 0L,
                lastPlayedAt = null,
            )
        )

        val result = useCase("Alice")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when name exists with different case`() = runTest {
        repository.players.add(
            SavedPlayer(
                id = PlayerId("existing-id"),
                name = PlayerName("Alice"),
                createdAt = 0L,
                lastPlayedAt = null,
            )
        )

        val result = useCase("alice")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should auto-select player by returning it in Result`() = runTest {
        val result = useCase("Alice")

        assertNotNull(result.getOrNull())
        assertEquals("Alice", result.getOrNull()?.name?.value)
    }
}
