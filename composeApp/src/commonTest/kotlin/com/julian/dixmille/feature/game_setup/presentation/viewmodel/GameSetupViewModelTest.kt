package com.julian.dixmille.feature.game_setup.presentation.viewmodel

import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.repository.FakeSavedPlayerRepository
import com.julian.dixmille.core.presentation.navigation.GameSetupNavigationEvent
import com.julian.dixmille.domain.usecase.FakeGameRepository
import com.julian.dixmille.domain.usecase.FakeGameRulesRepository
import com.julian.dixmille.feature.game_setup.domain.usecase.AddSavedPlayerUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.CreateGameUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.GetSavedPlayersUseCase
import com.julian.dixmille.feature.game_setup.presentation.model.GameSetupEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun savedPlayer(id: String, name: String): SavedPlayer = SavedPlayer(
        id = PlayerId(id),
        name = PlayerName(name),
        createdAt = 0L,
        lastPlayedAt = null,
    )

    private fun createViewModel(
        repo: FakeSavedPlayerRepository = FakeSavedPlayerRepository(),
    ): GameSetupViewModel {
        val getSavedPlayers = GetSavedPlayersUseCase(repo)
        val addSavedPlayer = AddSavedPlayerUseCase(repo, generateId = { "test-id" }, clock = { 0L })
        val gameRepo = FakeGameRepository()
        val rulesRepo = FakeGameRulesRepository()
        val createGame = CreateGameUseCase(gameRepo, rulesRepo)
        return GameSetupViewModel(
            createGameUseCase = createGame,
            gameRulesRepository = rulesRepo,
            getSavedPlayersUseCase = getSavedPlayers,
            addSavedPlayerUseCase = addSavedPlayer,
        )
    }

    // ── INCREMENT 12: Selection / deselection ─────────────────────────────────

    @Test
    fun `should load all players from repository on init`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("1", "Alice"))
        repo.players.add(savedPlayer("2", "Bob"))

        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.allPlayers.size)
    }

    @Test
    fun `should show player selector when ShowPlayerSelector event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.ShowPlayerSelector)

        assertTrue(viewModel.state.value.showPlayerSelector)
    }

    @Test
    fun `should hide player selector when HidePlayerSelector event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.ShowPlayerSelector)

        viewModel.onEvent(GameSetupEvent.HidePlayerSelector)

        assertFalse(viewModel.state.value.showPlayerSelector)
    }

    @Test
    fun `should add player to selected list when SelectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")

        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        assertTrue(viewModel.state.value.selectedPlayers.contains(alice))
    }

    @Test
    fun `should remove player from selected list when DeselectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        viewModel.onEvent(GameSetupEvent.DeselectPlayer("1"))

        assertTrue(viewModel.state.value.selectedPlayers.isEmpty())
    }

    @Test
    fun `should keep selected players in alphabetical order`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("1", "Zara")))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("2", "Alice")))

        assertEquals("Alice", viewModel.state.value.selectedPlayers[0].name.value)
    }

    @Test
    fun `should not add player when max players already selected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        (1..6).forEach { i -> viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("$i", "Player$i"))) }

        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("7", "Extra")))

        assertEquals(6, viewModel.state.value.selectedPlayers.size)
    }

    @Test
    fun `should close selector and update selection when ConfirmPlayerSelection received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.ShowPlayerSelector)
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(listOf(alice, bob)))

        assertFalse(viewModel.state.value.showPlayerSelector)
        assertEquals(2, viewModel.state.value.selectedPlayers.size)
        assertTrue(viewModel.state.value.selectedPlayers.any { it.id.value == "1" })
        assertTrue(viewModel.state.value.selectedPlayers.any { it.id.value == "2" })
    }

    // ── INCREMENT 13: Quick-add ───────────────────────────────────────────────

    @Test
    fun `should add new player to allPlayers and selectedPlayers when QuickAddPlayer succeeds`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Charlie"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.allPlayers.any { it.name.value == "Charlie" })
        assertTrue(viewModel.state.value.selectedPlayers.any { it.name.value == "Charlie" })
    }

    @Test
    fun `should set quickAddError when QuickAddPlayer fails with duplicate name`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("existing", "Alice"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Alice"))
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.quickAddError)
    }

    @Test
    fun `should clear unifiedInput after successful QuickAddPlayer via legacy path`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("Charlie"))

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Charlie"))
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.unifiedInput)
    }

    // ── INCREMENT 14: Search / filter ─────────────────────────────────────────

    @Test
    fun `should filter allPlayers by unified input when UpdateUnifiedInput received`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("1", "Alice"))
        repo.players.add(savedPlayer("2", "Bob"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        assertEquals(listOf("Alice"), viewModel.state.value.filteredPlayers.map { it.name.value })
    }

    @Test
    fun `should return all players when unified input is cleared`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("1", "Alice"))
        repo.players.add(savedPlayer("2", "Bob"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput(""))

        assertEquals(2, viewModel.state.value.filteredPlayers.size)
    }

    @Test
    fun `should preserve selected state of filtered-out players`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        repo.players.add(alice)
        repo.players.add(savedPlayer("2", "Bob"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("bob"))

        assertTrue(viewModel.state.value.selectedPlayers.any { it.id.value == "1" })
    }

    // ── INCREMENT 18: CreateGame ──────────────────────────────────────────────

    @Test
    fun `should navigate to score sheet when CreateGame succeeds with 2 or more players`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("1", "Alice")))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("2", "Bob")))

        var navigated = false
        val job = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.navigationEvents.collect { event ->
                if (event is GameSetupNavigationEvent.NavigateToScoreSheet) {
                    navigated = true
                }
            }
        }

        viewModel.onEvent(GameSetupEvent.CreateGame)
        advanceUntilIdle()

        assertTrue(navigated)
        job.cancel()
    }

    @Test
    fun `should set error when CreateGame triggered with fewer than 2 players`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("1", "Alice")))

        viewModel.onEvent(GameSetupEvent.CreateGame)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
    }

    // ── INCREMENT 2: Unified input ────────────────────────────────────────────

    @Test
    fun `should update unifiedInput when UpdateUnifiedInput event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        assertEquals("ali", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should clear unifiedInput when UpdateUnifiedInput received with empty string`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput(""))

        assertEquals("", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should clear unifiedInput when HidePlayerSelector event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.HidePlayerSelector)

        assertEquals("", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should clear unifiedInput when ConfirmPlayerSelection event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(emptyList()))

        assertEquals("", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should clear unifiedInput after QuickAddPlayer succeeds`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("Charlie"))

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Charlie"))
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should not change unifiedInput when SelectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("1", "Alice")))

        assertEquals("ali", viewModel.state.value.unifiedInput)
    }

    @Test
    fun `should not change unifiedInput when DeselectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("ali"))

        viewModel.onEvent(GameSetupEvent.DeselectPlayer("1"))

        assertEquals("ali", viewModel.state.value.unifiedInput)
    }
}
