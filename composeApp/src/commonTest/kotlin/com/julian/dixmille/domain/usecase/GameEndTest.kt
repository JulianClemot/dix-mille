package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
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
        bustTurnUseCase = BustTurnUseCase(repository, ScoreValidator())
        skipTurnUseCase = SkipTurnUseCase(repository, ScoreValidator())
    }

    @Test
    fun `Should return highest scoring player when game ended`() = runTest {
        // Arrange
        val alice = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(12500), hasEnteredGame = true)
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(10800), hasEnteredGame = true)
        val carol = Player(id = PlayerId("p3"), name = PlayerName("Carol"), totalScore = Score(9200), hasEnteredGame = true)
        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob, carol),
            targetScore = TargetScore(10_000),
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
    fun `Should return null when game has not ended`() = runTest {
        // Arrange
        val alice = Player(id = PlayerId("p1"), name = PlayerName("Alice"), totalScore = Score(12500), hasEnteredGame = true)
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(10800), hasEnteredGame = true)
        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
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
    fun `Should reject bust when game has ended`() = runTest {
        // Arrange
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10500),
            hasEnteredGame = true,
            currentTurn = Turn(id = TurnId(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(9000), hasEnteredGame = true)
        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
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
    fun `Should reject skip when game has ended`() = runTest {
        // Arrange
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10500),
            hasEnteredGame = true,
            currentTurn = Turn(id = TurnId(UuidGenerator.generate()))
        )
        val bob = Player(id = PlayerId("p2"), name = PlayerName("Bob"), totalScore = Score(9000), hasEnteredGame = true)
        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob),
            targetScore = TargetScore(10_000),
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
