package com.julian.dixmille.domain.usecase

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.TurnId
import com.julian.dixmille.core.domain.util.UuidGenerator
import com.julian.dixmille.core.domain.service.ScoreValidator
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryRuleTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: CommitTurnUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = CommitTurnUseCase(repository, ScoreValidator())
    }

    // ── Boundary values ───────────────────────────────────────────────────────

    @Test
    fun `Should enter game when turn total is exactly 500`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 500))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(500), player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should reject commit when not entered and turn total is below 500`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 450))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(0), player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `Should enter game when turn total exceeds 500`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 750))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(750), player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should set hasEnteredGame when minimum score met`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 500))

        useCase()

        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertTrue(player.hasEnteredGame)
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `Should reject commit when not entered and turn is below minimum`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 400))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(0), player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── State preconditions ───────────────────────────────────────────────────

    @Test
    fun `Should allow any score when already entered game`() = runTest {
        repository.saveGame(gameForEntered(totalScore = Score.of(600), turnPoints = 50))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(650), player.totalScore)
    }

    // ── Configurable entry minimum ────────────────────────────────────────────

    @Test
    fun `Should enter game when custom minimum met exactly`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 300, entryMinimum = 300))

        val result = useCase()

        assertTrue(result.isSuccess)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(300), player.totalScore)
        assertTrue(player.hasEnteredGame)
    }

    @Test
    fun `Should reject commit when custom minimum not met`() = runTest {
        repository.saveGame(gameForUnentered(turnPoints = 250, entryMinimum = 300))

        val result = useCase()

        assertTrue(result.isFailure)
        val player = repository.getCurrentGame().getOrThrow().players[0]
        assertEquals(Score.of(0), player.totalScore)
        assertFalse(player.hasEnteredGame)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameForUnentered(turnPoints: Int, entryMinimum: Int = 500): Game {
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(0),
            hasEnteredGame = false,
            currentTurn = turnOf(turnPoints)
        )
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L,
            rules = GameRules.DEFAULT.copy(entryMinimumScore = EntryMinimumScore.of(entryMinimum))
        )
    }

    private fun gameForEntered(totalScore: Score, turnPoints: Int): Game {
        val alice = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = totalScore,
            hasEnteredGame = true,
            currentTurn = turnOf(turnPoints)
        )
        return Game(
            id = GameId.of("game1"),
            players = listOf(alice, Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob"))),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            createdAt = 0L
        )
    }

    private fun turnOf(points: Int): Turn = Turn(
        id = TurnId.of(UuidGenerator.generate()),
        entries = listOf(ScoreEntry(id = EntryId.of(UuidGenerator.generate()), points = Score.of(points)))
    )
}
