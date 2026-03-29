package com.julian.dixmille.data.mapper

import com.julian.dixmille.core.data.mapper.toDomain
import com.julian.dixmille.core.data.mapper.toDto
import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.ScoreType
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnOutcome
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameMapperTest {

    @Test
    fun `Should round-trip Game through DTO without data loss`() {
        // Arrange
        val player1 = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(1500),
            hasEnteredGame = true,
            currentTurn = null,
            hasPlayedFinalRound = false,
            consecutiveBusts = BustCount.NONE
        )
        val player2 = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.ZERO,
            hasEnteredGame = false,
            currentTurn = null,
            hasPlayedFinalRound = false,
            consecutiveBusts = BustCount.NONE
        )
        val game = Game(
            id = GameId.of("game-1"),
            players = listOf(player1, player2),
            targetScore = TargetScore.of(10_000),
            currentPlayerIndex = 0,
            gamePhase = GamePhase.IN_PROGRESS,
            triggeringPlayerId = null,
            createdAt = 1234567890L,
            turnHistory = emptyList(),
            roundNumber = 3,
            rules = GameRules.DEFAULT
        )

        // Act
        val roundTripped = game.toDto().toDomain()

        // Assert
        assertEquals(game.id.value, roundTripped.id.value)
        assertEquals(2, roundTripped.players.size)
        assertEquals("p1", roundTripped.players[0].id.value)
        assertEquals("Alice", roundTripped.players[0].name.value)
        assertEquals(1500, roundTripped.players[0].totalScore.value)
        assertEquals(true, roundTripped.players[0].hasEnteredGame)
        assertEquals(game.targetScore.value, roundTripped.targetScore.value)
        assertEquals(game.currentPlayerIndex, roundTripped.currentPlayerIndex)
        assertEquals(game.gamePhase, roundTripped.gamePhase)
        assertNull(roundTripped.triggeringPlayerId)
        assertEquals(game.createdAt, roundTripped.createdAt)
        assertEquals(game.roundNumber, roundTripped.roundNumber)
    }

    @Test
    fun `Should round-trip GameRules through DTO without data loss`() {
        // Arrange
        val rules = GameRules(
            targetScore = TargetScore.of(5_000),
            entryMinimumScore = EntryMinimumScore.of(300),
            consecutiveBustsForPenalty = 4,
            minPlayers = 3,
            maxPlayers = 8,
            enableBustPenalty = false,
            enableFinalRound = false
        )

        // Act
        val roundTripped = rules.toDto().toDomain()

        // Assert
        assertEquals(rules.targetScore.value, roundTripped.targetScore.value)
        assertEquals(rules.entryMinimumScore.value, roundTripped.entryMinimumScore.value)
        assertEquals(rules.consecutiveBustsForPenalty, roundTripped.consecutiveBustsForPenalty)
        assertEquals(rules.minPlayers, roundTripped.minPlayers)
        assertEquals(rules.maxPlayers, roundTripped.maxPlayers)
        assertEquals(rules.enableBustPenalty, roundTripped.enableBustPenalty)
        assertEquals(rules.enableFinalRound, roundTripped.enableFinalRound)
    }

    @Test
    fun `Should preserve all Player fields through DTO mapping`() {
        // Arrange
        val turn = Turn(
            id = TurnId.of("turn-1"),
            entries = listOf(
                ScoreEntry(
                    id = EntryId.of("entry-1"),
                    points = Score.of(500),
                    type = ScoreType.PRESET,
                    label = null
                )
            ),
            isBusted = false
        )
        val player = Player(
            id = PlayerId.of("p2"),
            name = PlayerName.of("Bob"),
            totalScore = Score.of(2500),
            hasEnteredGame = true,
            currentTurn = turn,
            hasPlayedFinalRound = true,
            consecutiveBusts = BustCount.of(2)
        )

        // Act
        val roundTripped = player.toDto().toDomain()

        // Assert
        assertEquals(player.id.value, roundTripped.id.value)
        assertEquals(player.name.value, roundTripped.name.value)
        assertEquals(player.totalScore.value, roundTripped.totalScore.value)
        assertEquals(player.hasEnteredGame, roundTripped.hasEnteredGame)
        assertEquals(player.hasPlayedFinalRound, roundTripped.hasPlayedFinalRound)
        assertEquals(player.consecutiveBusts.value, roundTripped.consecutiveBusts.value)
        assertEquals(turn.id.value, roundTripped.currentTurn?.id?.value)
        assertEquals(turn.entries.size, roundTripped.currentTurn?.entries?.size)
    }

    @Test
    fun `Should preserve Turn entries through DTO mapping`() {
        // Arrange
        val turn = Turn(
            id = TurnId.of("turn-42"),
            entries = listOf(
                ScoreEntry(
                    id = EntryId.of("e1"),
                    points = Score.of(100),
                    type = ScoreType.PRESET,
                    label = null
                ),
                ScoreEntry(
                    id = EntryId.of("e2"),
                    points = Score.of(200),
                    type = ScoreType.CUSTOM,
                    label = "special"
                )
            ),
            isBusted = true
        )

        // Act
        val roundTripped = turn.toDto().toDomain()

        // Assert
        assertEquals(turn.id.value, roundTripped.id.value)
        assertEquals(turn.isBusted, roundTripped.isBusted)
        assertEquals(2, roundTripped.entries.size)
        assertEquals("e1", roundTripped.entries[0].id.value)
        assertEquals(100, roundTripped.entries[0].points.value)
        assertEquals(ScoreType.PRESET, roundTripped.entries[0].type)
        assertNull(roundTripped.entries[0].label)
        assertEquals("e2", roundTripped.entries[1].id.value)
        assertEquals(200, roundTripped.entries[1].points.value)
        assertEquals(ScoreType.CUSTOM, roundTripped.entries[1].type)
        assertEquals("special", roundTripped.entries[1].label)
    }

    @Test
    fun `Should preserve TurnRecord fields through DTO mapping`() {
        // Arrange
        val record = TurnRecord(
            roundNumber = 5,
            playerId = PlayerId.of("p3"),
            points = Score.of(750),
            outcome = TurnOutcome.SCORED,
            previousScore = Score.of(1000)
        )

        // Act
        val roundTripped = record.toDto().toDomain()

        // Assert
        assertEquals(record.roundNumber, roundTripped.roundNumber)
        assertEquals(record.playerId.value, roundTripped.playerId.value)
        assertEquals(record.points.value, roundTripped.points.value)
        assertEquals(record.outcome, roundTripped.outcome)
        assertEquals(record.previousScore.value, roundTripped.previousScore.value)
    }
}
