package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GamePhase
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameVoFieldsTest {

    @Test
    fun `Should create Game with GameId and TargetScore`() {
        // Arrange
        val players = listOf(
            Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice")),
            Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob")),
        )

        // Act
        val game = Game(
            id = GameId.of("game-abc"),
            players = players,
            targetScore = TargetScore.of(5_000),
            createdAt = 0L,
        )

        // Assert
        assertEquals("game-abc", game.id.value)
        assertEquals(5_000, game.targetScore.value)
    }

    @Test
    fun `Should store triggeringPlayerId as PlayerId`() {
        // Arrange
        val players = listOf(
            Player(id = PlayerId.of("p1"), name = PlayerName.of("Alice")),
            Player(id = PlayerId.of("p2"), name = PlayerName.of("Bob")),
        )

        // Act
        val game = Game(
            id = GameId.of("game-xyz"),
            players = players,
            targetScore = TargetScore.of(10_000),
            triggeringPlayerId = PlayerId.of("p1"),
            gamePhase = GamePhase.FINAL_ROUND,
            createdAt = 0L,
        )

        // Assert
        assertEquals("p1", game.triggeringPlayerId?.value)

        val gameNoTrigger = game.copy(triggeringPlayerId = null)
        assertNull(gameNoTrigger.triggeringPlayerId)
    }

    @Test
    fun `Should create GameRules with TargetScore and EntryMinimumScore`() {
        // Arrange / Act
        val rules = GameRules(
            targetScore = TargetScore.of(5_000),
            entryMinimumScore = EntryMinimumScore.of(300),
        )

        // Assert
        assertEquals(5_000, rules.targetScore.value)
        assertEquals(300, rules.entryMinimumScore.value)
    }
}
