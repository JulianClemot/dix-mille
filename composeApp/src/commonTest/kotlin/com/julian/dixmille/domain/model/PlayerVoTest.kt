package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerVoTest {

    @Test
    fun `Should create Player with PlayerId and PlayerName`() {
        // Arrange & Act
        val player = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice")
        )

        // Assert
        assertEquals(PlayerId.of("p1"), player.id)
        assertEquals(PlayerName.of("Alice"), player.name)
    }

    @Test
    fun `Should create Player with Score totalScore and BustCount consecutiveBusts`() {
        // Arrange & Act
        val player = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice"),
            totalScore = Score.of(1000),
            consecutiveBusts = BustCount.of(2)
        )

        // Assert
        assertEquals(Score.of(1000), player.totalScore)
        assertEquals(BustCount.of(2), player.consecutiveBusts)
    }

    @Test
    fun `Should commit turn using EntryMinimumScore`() {
        // Arrange
        val player = Player(
            id = PlayerId.of("p1"),
            name = PlayerName.of("Alice")
        )
            .startTurn(TurnId.of("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId.of("e1"), points = Score.of(500)))

        // Act
        val updated = player.commitTurn(entryMinimumScore = EntryMinimumScore.DEFAULT)

        // Assert
        assertTrue(updated.hasEnteredGame)
        assertEquals(Score.of(500), updated.totalScore)
    }
}
