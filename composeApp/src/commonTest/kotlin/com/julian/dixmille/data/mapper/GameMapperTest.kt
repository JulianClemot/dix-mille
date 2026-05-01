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
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(1500),
            hasEnteredGame = true,
            currentTurn = null,
            hasPlayedFinalRound = false,
            consecutiveBusts = BustCount.NONE
        )
        val player2 = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score.ZERO,
            hasEnteredGame = false,
            currentTurn = null,
            hasPlayedFinalRound = false,
            consecutiveBusts = BustCount.NONE
        )
        val game = Game(
            id = GameId("game-1"),
            players = listOf(player1, player2),
            targetScore = TargetScore(10_000),
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
            targetScore = TargetScore(5_000),
            entryMinimumScore = EntryMinimumScore(300),
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
            id = TurnId("turn-1"),
            entries = listOf(
                ScoreEntry(
                    id = EntryId("entry-1"),
                    points = Score(500),
                    type = ScoreType.PRESET,
                    label = null
                )
            ),
            isBusted = false
        )
        val player = Player(
            id = PlayerId("p2"),
            name = PlayerName("Bob"),
            totalScore = Score(2500),
            hasEnteredGame = true,
            currentTurn = turn,
            hasPlayedFinalRound = true,
            consecutiveBusts = BustCount(2)
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
            id = TurnId("turn-42"),
            entries = listOf(
                ScoreEntry(
                    id = EntryId("e1"),
                    points = Score(100),
                    type = ScoreType.PRESET,
                    label = null
                ),
                ScoreEntry(
                    id = EntryId("e2"),
                    points = Score(200),
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
        val expectedEntry0 = ScoreEntry(
            id = EntryId("e1"),
            points = Score(100),
            type = ScoreType.PRESET,
            label = null
        )
        assertEquals(expectedEntry0, roundTripped.entries[0])
        val expectedEntry1 = ScoreEntry(
            id = EntryId("e2"),
            points = Score(200),
            type = ScoreType.CUSTOM,
            label = "special"
        )
        assertEquals(expectedEntry1, roundTripped.entries[1])
    }

    @Test
    fun `Should preserve TurnRecord fields through DTO mapping`() {
        // Arrange
        val record = TurnRecord(
            roundNumber = 5,
            playerId = PlayerId("p3"),
            points = Score(750),
            outcome = TurnOutcome.SCORED,
            previousScore = Score(1000)
        )

        // Act
        val roundTripped = record.toDto().toDomain()

        // Assert
        assertEquals(record, roundTripped)
    }
}
