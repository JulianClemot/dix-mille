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
import com.julian.dixmille.feature.game_setup.domain.usecase.DeleteSavedPlayerUseCase
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
        val deleteSavedPlayer = DeleteSavedPlayerUseCase(repo)
        val gameRepo = FakeGameRepository()
        val rulesRepo = FakeGameRulesRepository()
        val createGame = CreateGameUseCase(gameRepo, rulesRepo)
        return GameSetupViewModel(
            createGameUseCase = createGame,
            gameRulesRepository = rulesRepo,
            getSavedPlayersUseCase = getSavedPlayers,
            addSavedPlayerUseCase = addSavedPlayer,
            deleteSavedPlayerUseCase = deleteSavedPlayer,
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
    fun `Should preserve selection order when SelectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")

        viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        assertEquals(listOf(zara, alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should append newly selected player to end of list preserving prior order`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        val mike = savedPlayer("3", "Mike")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        viewModel.onEvent(GameSetupEvent.SelectPlayer(mike))

        assertEquals(listOf(zara, alice, mike), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should contain single player when only one SelectPlayer event received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")

        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        assertEquals(listOf(alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should preserve selection order at max player cap`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val players = listOf(
            savedPlayer("1", "Zoe"),
            savedPlayer("2", "Mike"),
            savedPlayer("3", "Alice"),
            savedPlayer("4", "Yara"),
            savedPlayer("5", "Bob"),
            savedPlayer("6", "Nina"),
        )

        players.forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        assertEquals(players, viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should not change order of existing selection when SelectPlayer exceeds max players`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val players = listOf(
            savedPlayer("1", "Zoe"),
            savedPlayer("2", "Mike"),
            savedPlayer("3", "Alice"),
            savedPlayer("4", "Yara"),
            savedPlayer("5", "Bob"),
            savedPlayer("6", "Nina"),
        )
        players.forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("7", "Extra")))

        assertEquals(players, viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should not affect allPlayers ordering when SelectPlayer event received`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        repo.players.add(zara)
        repo.players.add(alice)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        val allPlayersBefore = viewModel.state.value.allPlayers

        viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))

        assertEquals(allPlayersBefore, viewModel.state.value.allPlayers)
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

    // ── INCREMENT 2 (Player Turn Order): Append order preservation ───────────

    @Test
    fun `Should preserve selection order when ConfirmPlayerSelection received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(viewModel.state.value.selectedPlayers))

        assertEquals(listOf(zara, alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should not sort selectedPlayers when ConfirmPlayerSelection receives unsorted list`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val mike = savedPlayer("1", "Mike")
        val alice = savedPlayer("2", "Alice")
        val zara = savedPlayer("3", "Zara")

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(listOf(mike, alice, zara)))

        assertEquals(listOf(mike, alice, zara), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should contain single player when ConfirmPlayerSelection receives one player`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(listOf(alice)))

        assertEquals(listOf(alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should preserve order at max player cap when ConfirmPlayerSelection received`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val players = listOf(
            savedPlayer("1", "Zoe"),
            savedPlayer("2", "Mike"),
            savedPlayer("3", "Alice"),
            savedPlayer("4", "Yara"),
            savedPlayer("5", "Bob"),
            savedPlayer("6", "Nina"),
        )

        viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(players))

        assertEquals(players, viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should append quick-added player to end of selectedPlayers preserving order`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val mike = savedPlayer("2", "Mike")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(mike))

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Alice"))
        advanceUntilIdle()

        val alice = viewModel.state.value.selectedPlayers.last()
        assertEquals(listOf(zara, mike, alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should keep allPlayers alphabetically sorted when QuickAddPlayer succeeds`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("1", "Zara"))
        repo.players.add(savedPlayer("2", "Mike"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Alice"))
        advanceUntilIdle()

        assertEquals(
            listOf("Alice", "Mike", "Zara"),
            viewModel.state.value.allPlayers.map { it.name.value },
        )
    }

    @Test
    fun `Should not add quick-added player to selectedPlayers when at max cap`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        val players = listOf(
            savedPlayer("1", "Zoe"),
            savedPlayer("2", "Mike"),
            savedPlayer("3", "Alice"),
            savedPlayer("4", "Yara"),
            savedPlayer("5", "Bob"),
            savedPlayer("6", "Nina"),
        )
        players.forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Extra"))
        advanceUntilIdle()

        assertEquals(players, viewModel.state.value.selectedPlayers)
        assertTrue(viewModel.state.value.allPlayers.any { it.name.value == "Extra" })
    }

    @Test
    fun `Should maintain cumulative order across SelectPlayer ConfirmPlayerSelection and QuickAddPlayer sequence`() =
        runTest {
            val repo = FakeSavedPlayerRepository()
            val viewModel = createViewModel(repo)
            advanceUntilIdle()
            val zara = savedPlayer("1", "Zara")
            viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))
            viewModel.onEvent(GameSetupEvent.ConfirmPlayerSelection(listOf(zara)))

            viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Mike"))
            advanceUntilIdle()

            val mike = viewModel.state.value.selectedPlayers.last()
            assertEquals(listOf(zara, mike), viewModel.state.value.selectedPlayers)
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

    // ── INCREMENT 3 (Player Turn Order): MovePlayer event ───────────────────

    @Test
    fun `Should move player down one position when MovePlayer fires with adjacent forward indices`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 1))

        assertEquals(listOf(bob, alice, carol), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should move player up one position when MovePlayer fires with adjacent backward indices`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 2, toIndex = 1))

        assertEquals(listOf(alice, carol, bob), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should move player across multiple positions when MovePlayer fires with non adjacent indices`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        val dave = savedPlayer("4", "Dave")
        listOf(alice, bob, carol, dave).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 2))

        assertEquals(listOf(bob, carol, alice, dave), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should promote last player to first position when MovePlayer moves last index to zero`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 2, toIndex = 0))

        assertEquals(listOf(carol, alice, bob), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should leave list unchanged when MovePlayer receives identical from and to index`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 1, toIndex = 1))

        assertEquals(listOf(alice, bob, carol), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should leave list unchanged when MovePlayer receives negative fromIndex`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = -1, toIndex = 1))

        assertEquals(listOf(alice, bob, carol), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should leave list unchanged when MovePlayer receives fromIndex equal to list size`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 3, toIndex = 0))

        assertEquals(listOf(alice, bob, carol), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should leave list unchanged when MovePlayer receives toIndex beyond list bounds`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 5))

        assertEquals(listOf(alice, bob, carol), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should leave list unchanged when MovePlayer fires on empty selection`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 0))

        assertTrue(viewModel.state.value.selectedPlayers.isEmpty())
    }

    @Test
    fun `Should leave list unchanged when MovePlayer fires on single player selection`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 0))

        assertEquals(listOf(alice), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should not change unrelated state fields when MovePlayer fires`() = runTest {
        val repo = FakeSavedPlayerRepository()
        repo.players.add(savedPlayer("10", "Xander"))
        repo.players.add(savedPlayer("20", "Yusuf"))
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.UpdateTargetScore("8000"))
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("test"))
        val allPlayersBefore = viewModel.state.value.allPlayers

        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 2))

        assertEquals(listOf(bob, carol, alice), viewModel.state.value.selectedPlayers)
        assertEquals("8000", viewModel.state.value.targetScore)
        assertEquals("test", viewModel.state.value.unifiedInput)
        assertEquals(allPlayersBefore, viewModel.state.value.allPlayers)
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.quickAddError)
    }

    @Test
    fun `Should reorder players correctly when list was built via SelectPlayer and QuickAddPlayer events`() =
        runTest {
            val repo = FakeSavedPlayerRepository()
            val viewModel = createViewModel(repo)
            advanceUntilIdle()
            val zara = savedPlayer("1", "Zara")
            viewModel.onEvent(GameSetupEvent.SelectPlayer(zara))

            viewModel.onEvent(GameSetupEvent.QuickAddPlayer("Mike"))
            advanceUntilIdle()

            val mike = viewModel.state.value.selectedPlayers.last()
            viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 1, toIndex = 0))

            assertEquals(listOf(mike, zara), viewModel.state.value.selectedPlayers)
        }

    // ── INCREMENT 8 (Player Turn Order): Remove/add edge cases ───────────────

    @Test
    fun `Should preserve manual order of remaining players when removing a middle player`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        val mike = savedPlayer("3", "Mike")
        val dave = savedPlayer("4", "Dave")
        listOf(zara, alice, mike, dave).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 3, toIndex = 0))

        viewModel.onEvent(GameSetupEvent.RemoveSelectedPlayer(playerId = zara.id.value))

        assertEquals(listOf(dave, alice, mike), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should promote second player to starting position when first player is removed`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val getSavedPlayers = GetSavedPlayersUseCase(repo)
        val addSavedPlayer = AddSavedPlayerUseCase(repo, generateId = { "test-id" }, clock = { 0L })
        val deleteSavedPlayer = DeleteSavedPlayerUseCase(repo)
        val gameRepo = FakeGameRepository()
        val rulesRepo = FakeGameRulesRepository()
        val createGameUseCase = CreateGameUseCase(gameRepo, rulesRepo)
        val viewModel = GameSetupViewModel(
            createGameUseCase = createGameUseCase,
            gameRulesRepository = rulesRepo,
            getSavedPlayersUseCase = getSavedPlayers,
            addSavedPlayerUseCase = addSavedPlayer,
            deleteSavedPlayerUseCase = deleteSavedPlayer,
        )
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        val dave = savedPlayer("3", "Dave")
        listOf(zara, alice, dave).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 2, toIndex = 0))
        // state is now [Dave, Zara, Alice]

        viewModel.onEvent(GameSetupEvent.RemoveSelectedPlayer(playerId = dave.id.value))

        assertEquals(zara, viewModel.state.value.selectedPlayers[0])

        viewModel.onEvent(GameSetupEvent.CreateGame)
        advanceUntilIdle()

        val createdGame = gameRepo.getCurrentGame().getOrNull()
        assertNotNull(createdGame)
        assertEquals("Zara", createdGame.currentPlayer.name.value)
    }

    @Test
    fun `Should append re-added player to end of list not original position`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }

        viewModel.onEvent(GameSetupEvent.RemoveSelectedPlayer(playerId = bob.id.value))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(bob))

        assertEquals(listOf(alice, carol, bob), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should preserve manual reorder when adding a player afterward`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        listOf(alice, bob, carol).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 0, toIndex = 2))
        val dave = savedPlayer("4", "Dave")

        viewModel.onEvent(GameSetupEvent.SelectPlayer(dave))

        assertEquals(listOf(bob, carol, alice, dave), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should reject seventh player without disturbing manually reordered selection`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val players = (1..6).map { savedPlayer("$it", "P$it") }
        players.forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 5, toIndex = 0))
        val reordered = viewModel.state.value.selectedPlayers

        viewModel.onEvent(GameSetupEvent.SelectPlayer(savedPlayer("7", "Extra")))

        assertEquals(6, viewModel.state.value.selectedPlayers.size)
        assertEquals(reordered, viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `Should preserve manual order after navigating to rules settings and refreshing`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val zara = savedPlayer("1", "Zara")
        val alice = savedPlayer("2", "Alice")
        val mike = savedPlayer("3", "Mike")
        listOf(zara, alice, mike).forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        viewModel.onEvent(GameSetupEvent.MovePlayer(fromIndex = 2, toIndex = 0))
        val reordered = viewModel.state.value.selectedPlayers

        viewModel.navigateToRulesSettings()
        viewModel.refreshRules()
        advanceUntilIdle()

        assertEquals(reordered, viewModel.state.value.selectedPlayers)
    }

    // ── INCREMENT 5 (Delete Saved Player): DeleteSavedPlayer event ───────────

    @Test
    fun `should remove player from allPlayers when unselected player is deleted`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        repo.players.add(alice)
        repo.players.add(bob)
        repo.players.add(carol)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(bob))

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(carol.id.value))
        advanceUntilIdle()

        assertEquals(listOf(alice, bob), viewModel.state.value.allPlayers)
        assertEquals(listOf(alice, bob), viewModel.state.value.selectedPlayers)
    }

    @Test
    fun `should cascade removal from selectedPlayers preserving turn order`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        repo.players.add(alice)
        repo.players.add(bob)
        repo.players.add(carol)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(carol))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(bob))

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertEquals(listOf(carol, bob), viewModel.state.value.selectedPlayers)
        assertFalse(viewModel.state.value.allPlayers.contains(alice))
    }

    @Test
    fun `should disable start game when deletion drops selection below minimum`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        repo.players.add(alice)
        repo.players.add(bob)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.SelectPlayer(alice))
        viewModel.onEvent(GameSetupEvent.SelectPlayer(bob))

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertEquals(listOf(bob), viewModel.state.value.selectedPlayers)
        assertFalse(viewModel.state.value.canConfirmSelection)
        assertFalse(viewModel.state.value.canStartGame)
    }

    @Test
    fun `should allow adding more players after deletion drops below cap`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val players = (1..6).map { savedPlayer("$it", "Player$it") }
        players.forEach { repo.players.add(it) }
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        players.forEach { viewModel.onEvent(GameSetupEvent.SelectPlayer(it)) }
        val alice = players.first()

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.selectedPlayers.size)
        assertTrue(viewModel.state.value.canAddMorePlayers)
    }

    @Test
    fun `should preserve filter text after deleting a player`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        val carol = savedPlayer("3", "Carol")
        repo.players.add(alice)
        repo.players.add(bob)
        repo.players.add(carol)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("a"))

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(carol.id.value))
        advanceUntilIdle()

        assertEquals("a", viewModel.state.value.unifiedInput)
        assertEquals(listOf(alice), viewModel.state.value.filteredPlayers)
    }

    @Test
    fun `should empty player list when deleting the last remaining player`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        repo.players.add(alice)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.allPlayers.isEmpty())
        assertTrue(viewModel.state.value.filteredPlayers.isEmpty())
    }

    @Test
    fun `should keep player and set error message when deletion fails`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        repo.players.add(alice)
        repo.deleteFailure = IllegalStateException("Delete failed")
        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.allPlayers.contains(alice))
        assertNotNull(viewModel.state.value.deleteErrorMessage)
    }

    @Test
    fun `should clear error message when a subsequent deletion succeeds`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        val bob = savedPlayer("2", "Bob")
        repo.players.add(alice)
        repo.players.add(bob)
        repo.deleteFailure = IllegalStateException("Delete failed")
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.deleteErrorMessage)
        repo.deleteFailure = null

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(bob.id.value))
        advanceUntilIdle()

        assertNull(viewModel.state.value.deleteErrorMessage)
    }

    @Test
    fun `should re-enable add button when the typed exact-match name is deleted`() = runTest {
        val repo = FakeSavedPlayerRepository()
        val alice = savedPlayer("1", "Alice")
        repo.players.add(alice)
        val viewModel = createViewModel(repo)
        advanceUntilIdle()
        viewModel.onEvent(GameSetupEvent.UpdateUnifiedInput("Alice"))
        assertFalse(viewModel.state.value.canAddNewPlayer)

        viewModel.onEvent(GameSetupEvent.DeleteSavedPlayer(alice.id.value))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canAddNewPlayer)
    }
}
