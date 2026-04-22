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
            id = PlayerId("p1"),
            name = PlayerName("Alice")
        )

        // Assert
        assertEquals(PlayerId("p1"), player.id)
        assertEquals(PlayerName("Alice"), player.name)
    }

    @Test
    fun `Should create Player with Score totalScore and BustCount consecutiveBusts`() {
        // Arrange & Act
        val player = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice"),
            totalScore = Score(1000),
            consecutiveBusts = BustCount(2)
        )

        // Assert
        assertEquals(Score(1000), player.totalScore)
        assertEquals(BustCount(2), player.consecutiveBusts)
    }

    @Test
    fun `Should commit turn using EntryMinimumScore`() {
        // Arrange
        val player = Player(
            id = PlayerId("p1"),
            name = PlayerName("Alice")
        )
            .startTurn(TurnId("turn1"))
            .addScoreEntry(ScoreEntry(id = EntryId("e1"), points = Score(500)))

        // Act
        val updated = player.commitTurn(entryMinimumScore = EntryMinimumScore.DEFAULT)

        // Assert
        assertTrue(updated.hasEnteredGame)
        assertEquals(Score(500), updated.totalScore)
    }
}
