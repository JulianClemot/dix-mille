package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GameFinalRoundCollisionTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var commitTurnUseCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        commitTurnUseCase = CommitTurnUseCase(repository, ScoreValidator())
    }

    @Test
    fun `Should not revert other player when scores match during FINAL_ROUND`() = runTest {
        // Arrange: FINAL_ROUND — Alice triggered at 10000, Bob at 7000, Carol (current) commits 7000
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(7000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )
        val carol = Player(
            id = PlayerId("p3"),
            name = PlayerName("Carol"),
            totalScore = Score(0),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )

        var carolWithTurn = carol.startTurn(TurnId(UuidGenerator.generate()))
        carolWithTurn = carolWithTurn.addScoreEntry(createScoreEntry(7000))

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob, carolWithTurn),
            currentPlayerIndex = 2,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(10000),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals(7000, result.players[1].totalScore.value) // Bob remains at 7000
        assertEquals(7000, result.players[2].totalScore.value) // Carol at 7000
    }

    @Test
    fun `Should not record collision in turn history during FINAL_ROUND`() = runTest {
        // Arrange: Same setup — FINAL_ROUND, Carol's 7000 matches Bob's 7000
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(7000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )
        val carol = Player(
            id = PlayerId("p3"),
            name = PlayerName("Carol"),
            totalScore = Score(0),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )

        var carolWithTurn = carol.startTurn(TurnId(UuidGenerator.generate()))
        carolWithTurn = carolWithTurn.addScoreEntry(createScoreEntry(7000))

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob, carolWithTurn),
            currentPlayerIndex = 2,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(10000),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: no COLLISION record in turn history
        val result = repository.getCurrentGame().getOrThrow()
        val collisionRecords = result.turnHistory.filter { it.outcome == TurnOutcome.COLLISION }
        assertEquals(0, collisionRecords.size)
    }

    @Test
    fun `Should not revert triggering player when another player reaches target score in FINAL_ROUND`() = runTest {
        // Arrange: FINAL_ROUND, Alice triggered at 10000, Bob (current) at 9000 scoring 1000 to reach 10000
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(10000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(9000),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )

        var bobWithTurn = bob.startTurn(TurnId(UuidGenerator.generate()))
        bobWithTurn = bobWithTurn.addScoreEntry(createScoreEntry(1000))

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bobWithTurn),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(10000),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: Alice stays at 10000; Bob reaches 10000; no COLLISION
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals(10000, result.players[0].totalScore.value) // Alice not reverted
        assertEquals(10000, result.players[1].totalScore.value) // Bob at 10000
        val collisionRecords = result.turnHistory.filter { it.outcome == TurnOutcome.COLLISION }
        assertEquals(0, collisionRecords.size)
    }

    @Test
    fun `Should not revert any player when multiple players share same score during FINAL_ROUND`() = runTest {
        // Arrange: FINAL_ROUND — Alice at 7000, Bob at 7000, Carol (current) commits 7000
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(7000),
            hasEnteredGame = true,
            hasPlayedFinalRound = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(7000),
            hasEnteredGame = true,
            hasPlayedFinalRound = true
        )
        val carol = Player(
            id = PlayerId("p3"),
            name = PlayerName("Carol"),
            totalScore = Score(0),
            hasEnteredGame = true,
            hasPlayedFinalRound = false
        )

        var carolWithTurn = carol.startTurn(TurnId(UuidGenerator.generate()))
        carolWithTurn = carolWithTurn.addScoreEntry(createScoreEntry(7000))

        // Alice is triggering player; Carol is current
        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bob, carolWithTurn),
            currentPlayerIndex = 2,
            gamePhase = GamePhase.FINAL_ROUND,
            triggeringPlayerId = PlayerId("p1"),
            targetScore = TargetScore(10000),
            createdAt = 0L,
            turnHistory = emptyList(),
            roundNumber = 2
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: all three at 7000; no COLLISION records
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals(7000, result.players[0].totalScore.value) // Alice unchanged
        assertEquals(7000, result.players[1].totalScore.value) // Bob unchanged
        assertEquals(7000, result.players[2].totalScore.value) // Carol at 7000
        val collisionRecords = result.turnHistory.filter { it.outcome == TurnOutcome.COLLISION }
        assertEquals(0, collisionRecords.size)
    }

    @Test
    fun `Should still revert other player on score collision during IN_PROGRESS`() = runTest {
        // Arrange: IN_PROGRESS — Alice at 500; Bob (current) scores 500 to match
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(500),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(0),
            hasEnteredGame = false
        )

        var bobWithTurn = bob.startTurn(TurnId(UuidGenerator.generate()))
        bobWithTurn = bobWithTurn.addScoreEntry(createScoreEntry(500))

        // Record Alice's previous scored turn so collision can find previousScore
        val aliceScoredRecord = com.julian.dixmille.core.domain.model.TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("p1"),
            points = Score(500),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(0)
        )

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bobWithTurn),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(aliceScoredRecord),
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: Alice reverted to 0; COLLISION record exists for Alice
        val result = repository.getCurrentGame().getOrThrow()
        assertEquals(0, result.players[0].totalScore.value) // Alice reverted
        val collisionRecords = result.turnHistory.filter { it.outcome == TurnOutcome.COLLISION }
        assertEquals(1, collisionRecords.size)
        assertEquals("p1", collisionRecords[0].playerId.value)
    }

    @Test
    fun `Should trigger collision in IN_PROGRESS regardless of score value`() = runTest {
        // Arrange: IN_PROGRESS — Alice at 9000; Bob (current) scores 500 to reach 9500 — wait,
        // we need Bob to reach exactly 9000, so Bob starts at 8500 and scores 500
        val alice = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(9000),
            hasEnteredGame = true
        )
        val bob = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(8500),
            hasEnteredGame = true
        )

        var bobWithTurn = bob.startTurn(TurnId(UuidGenerator.generate()))
        bobWithTurn = bobWithTurn.addScoreEntry(createScoreEntry(500))

        // Record Alice's previous scored turn
        val aliceScoredRecord = com.julian.dixmille.core.domain.model.TurnRecord(
            roundNumber = 1,
            playerId = PlayerId("p1"),
            points = Score(9000),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(0)
        )

        val game = Game(
            id = GameId("game1"),
            players = listOf(alice, bobWithTurn),
            currentPlayerIndex = 1,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            turnHistory = listOf(aliceScoredRecord),
            roundNumber = 1
        )
        repository.saveGame(game)

        // Act
        commitTurnUseCase()

        // Assert: Alice reverted (score goes below 9000); COLLISION record exists
        val result = repository.getCurrentGame().getOrThrow()
        assertFalse(result.players[0].totalScore.value >= 9000) // Alice reverted below 9000
        val collisionRecords = result.turnHistory.filter { it.outcome == TurnOutcome.COLLISION }
        assertEquals(1, collisionRecords.size)
        assertEquals("p1", collisionRecords[0].playerId.value)
    }

    private fun createScoreEntry(points: Int): ScoreEntry {
        return ScoreEntry(
            id = EntryId(UuidGenerator.generate()),
            points = Score(points)
        )
    }
}
