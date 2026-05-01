package com.julian.dixmille.feature.game_end.presentation.viewmodel

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.usecase.GetCurrentGameUseCase
import com.julian.dixmille.domain.usecase.FakeGameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameEndViewModelTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var viewModel: GameEndViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = FakeGameRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun makePlayer(id: String, name: String, totalScore: Int): Player =
        Player(
            id = PlayerId(id),
            name = PlayerName(name),
            totalScore = Score(totalScore),
            hasEnteredGame = totalScore > 0
        )

    private fun makeCrossingRecord(
        playerId: String,
        previousScore: Int,
        points: Int,
        roundNumber: Int = 1
    ): TurnRecord = TurnRecord(
        roundNumber = roundNumber,
        playerId = PlayerId(playerId),
        points = Score(points),
        outcome = TurnOutcome.SCORED,
        previousScore = Score(previousScore)
    )

    private fun makeEndedGame(
        players: List<Player>,
        turnHistory: List<TurnRecord> = emptyList(),
        targetScore: Int = 10_000
    ): Game = Game(
        id = GameId("game1"),
        players = players,
        targetScore = TargetScore(targetScore),
        currentPlayerIndex = 0,
        gamePhase = GamePhase.ENDED,
        createdAt = 0L,
        turnHistory = turnHistory,
        roundNumber = 2,
        rules = GameRules()
    )

    // ---------------------------------------------------------------------------
    // Test 1: Expose ranked players in state after loading
    // ---------------------------------------------------------------------------

    @Test
    fun `Should expose ranked players in state after loading`() = runTest {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeEndedGame(
            players = listOf(alice, bob),
            turnHistory = listOf(aliceCrossing)
        )
        repository.saveGame(game)
        viewModel = GameEndViewModel(GetCurrentGameUseCase(repository), repository)

        // Act
        viewModel.loadFinalRankings()
        advanceUntilIdle()

        // Assert
        val rankedPlayers = viewModel.state.value.rankedPlayers
        assertEquals(2, rankedPlayers.size)
        assertEquals(alice, rankedPlayers[0].player)
        assertEquals(1, rankedPlayers[0].rank)
        assertEquals(bob, rankedPlayers[1].player)
        assertEquals(2, rankedPlayers[1].rank)
    }

    // ---------------------------------------------------------------------------
    // Test 2: Tied players share the same rank
    // ---------------------------------------------------------------------------

    @Test
    fun `Should assign same rank to tied players in state`() = runTest {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val carol = makePlayer("p3", "Carol", 8_500)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeEndedGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossing)
        )
        repository.saveGame(game)
        viewModel = GameEndViewModel(GetCurrentGameUseCase(repository), repository)

        // Act
        viewModel.loadFinalRankings()
        advanceUntilIdle()

        // Assert
        val rankedPlayers = viewModel.state.value.rankedPlayers
        assertEquals(3, rankedPlayers.size)
        val bobEntry = rankedPlayers.first { it.player == bob }
        val carolEntry = rankedPlayers.first { it.player == carol }
        assertEquals(2, bobEntry.rank)
        assertEquals(2, carolEntry.rank)
    }

    // ---------------------------------------------------------------------------
    // Test 3: isLoading is false after rankings loaded
    // ---------------------------------------------------------------------------

    @Test
    fun `Should set isLoading false after rankings loaded`() = runTest {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 8_500)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500)
        val game = makeEndedGame(
            players = listOf(alice, bob),
            turnHistory = listOf(aliceCrossing)
        )
        repository.saveGame(game)
        viewModel = GameEndViewModel(GetCurrentGameUseCase(repository), repository)

        // Act
        viewModel.loadFinalRankings()
        advanceUntilIdle()

        // Assert
        assertEquals(false, viewModel.state.value.isLoading)
    }

    // ---------------------------------------------------------------------------
    // Test 4: Empty ranked players on load failure
    // ---------------------------------------------------------------------------

    @Test
    fun `Should set empty ranked players on load failure`() = runTest {
        // Arrange — repository has no game saved
        viewModel = GameEndViewModel(GetCurrentGameUseCase(repository), repository)

        // Act
        viewModel.loadFinalRankings()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.state.value.rankedPlayers.isEmpty())
        assertEquals(false, viewModel.state.value.isLoading)
    }

    // ---------------------------------------------------------------------------
    // Test 5: Target-reaching players ordered before below-target players
    // ---------------------------------------------------------------------------

    @Test
    fun `Should order target-reaching players before below-target players in state`() = runTest {
        // Arrange
        val alice = makePlayer("p1", "Alice", 10_000)
        val bob = makePlayer("p2", "Bob", 10_500)
        val carol = makePlayer("p3", "Carol", 9_500)
        // Alice crossed target first (index 0), Bob crossed second (index 1)
        val aliceCrossing = makeCrossingRecord(playerId = "p1", previousScore = 9_500, points = 500, roundNumber = 1)
        val bobCrossing = makeCrossingRecord(playerId = "p2", previousScore = 9_500, points = 1_000, roundNumber = 1)
        val game = makeEndedGame(
            players = listOf(alice, bob, carol),
            turnHistory = listOf(aliceCrossing, bobCrossing)
        )
        repository.saveGame(game)
        viewModel = GameEndViewModel(GetCurrentGameUseCase(repository), repository)

        // Act
        viewModel.loadFinalRankings()
        advanceUntilIdle()

        // Assert
        val rankedPlayers = viewModel.state.value.rankedPlayers
        assertEquals(3, rankedPlayers.size)
        assertEquals(alice, rankedPlayers[0].player)
        assertEquals(bob, rankedPlayers[1].player)
        assertEquals(carol, rankedPlayers[2].player)
    }
}
