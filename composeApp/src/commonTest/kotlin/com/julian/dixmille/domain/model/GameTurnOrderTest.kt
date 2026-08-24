package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.TargetScore
import kotlin.test.Test
import kotlin.test.assertEquals

class GameTurnOrderTest {

    @Test
    fun `Should rotate turns in reordered list order and wrap to start`() {
        // Arrange
        val players = listOf(
            Player(id = PlayerId("p-carol"), name = PlayerName("Carol")),
            Player(id = PlayerId("p-alice"), name = PlayerName("Alice")),
            Player(id = PlayerId("p-bob"), name = PlayerName("Bob")),
        )
        var game = Game(
            id = GameId("game-turn-order"),
            players = players,
            targetScore = TargetScore.DEFAULT,
            currentPlayerIndex = 0,
            createdAt = 0L,
        )

        // Act & Assert
        game = game.advanceToNextPlayer()
        assertEquals("Alice", game.currentPlayer.name.value)

        game = game.advanceToNextPlayer()
        assertEquals("Bob", game.currentPlayer.name.value)

        game = game.advanceToNextPlayer()
        assertEquals("Carol", game.currentPlayer.name.value)
    }
}
