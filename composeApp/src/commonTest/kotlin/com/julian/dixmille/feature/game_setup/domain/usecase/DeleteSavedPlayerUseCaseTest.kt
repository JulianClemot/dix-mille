package com.julian.dixmille.feature.game_setup.domain.usecase

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteSavedPlayerUseCaseTest {

    private val repository = FakeSavedPlayerRepository()
    private val useCase = DeleteSavedPlayerUseCase(repository)

    @Test
    fun `should return success when repository deletion succeeds`() = runTest {
        repository.players.add(
            SavedPlayer(
                id = PlayerId("existing-id"),
                name = PlayerName("Alice"),
                createdAt = 0L,
                lastPlayedAt = null,
            )
        )

        val result = useCase("existing-id")

        assertTrue(result.isSuccess)
        assertEquals(0, repository.players.size)
    }

    @Test
    fun `should return failure when repository deletion fails`() = runTest {
        val exception = IllegalStateException("Delete failed")
        repository.deleteFailure = exception

        val result = useCase("existing-id")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
