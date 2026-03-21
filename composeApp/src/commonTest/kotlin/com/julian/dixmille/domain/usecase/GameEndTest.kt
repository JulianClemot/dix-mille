package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEndTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var bustTurnUseCase: BustTurnUseCase
    private lateinit var skipTurnUseCase: SkipTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        bustTurnUseCase = BustTurnUseCase(repository)
        skipTurnUseCase = SkipTurnUseCase(repository)
    }

    @Test
    fun should_returnHighestScoringPlayer_when_gameEnded() = runTest {
        // Arrange
        val alice = Player(id = "p1", name = "Alice", totalScore = 12500, hasEnteredGame = true)
        val bob = Player(id = "p2", name = "Bob", totalScore = 10800, hasEnteredGame = true)
        val carol = Player(id = "p3", name = "Carol", totalScore = 9200, hasEnteredGame = true)
        val game = Game(
            id = "game1",
            players = listOf(alice, bob, carol),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            createdAt = 0L
        )

        // Act
        val winner = game.getWinner()

        // Assert
        assertEquals(alice, winner)
    }

    @Test
    fun should_returnNull_when_gameNotEnded() = runTest {
        // Arrange
        val alice = Player(id = "p1", name = "Alice", totalScore = 12500, hasEnteredGame = true)
        val bob = Player(id = "p2", name = "Bob", totalScore = 10800, hasEnteredGame = true)
        val game = Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )

        // Act
        val winner = game.getWinner()

        // Assert
        assertNull(winner)
    }

    @Test
    fun should_rejectBust_when_gameHasEnded() = runTest {
        // Arrange
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = 10500,
            hasEnteredGame = true,
            currentTurn = Turn(id = UuidGenerator.generate())
        )
        val bob = Player(id = "p2", name = "Bob", totalScore = 9000, hasEnteredGame = true)
        val game = Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            createdAt = 0L
        )
        repository.saveGame(game)

        // Act
        val result = bustTurnUseCase()

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun should_rejectSkip_when_gameHasEnded() = runTest {
        // Arrange
        val alice = Player(
            id = "p1",
            name = "Alice",
            totalScore = 10500,
            hasEnteredGame = true,
            currentTurn = Turn(id = UuidGenerator.generate())
        )
        val bob = Player(id = "p2", name = "Bob", totalScore = 9000, hasEnteredGame = true)
        val game = Game(
            id = "game1",
            players = listOf(alice, bob),
            targetScore = 10_000,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.ENDED,
            createdAt = 0L
        )
        repository.saveGame(game)

        // Act
        val result = skipTurnUseCase()

        // Assert
        assertTrue(result.isFailure)
    }
}
