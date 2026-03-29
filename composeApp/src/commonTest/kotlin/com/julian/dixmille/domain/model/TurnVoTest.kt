package com.julian.dixmille.domain.model

import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.vo.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnVoTest {

    @Test
    fun `Should create Turn with TurnId`() {
        // Arrange
        val id = TurnId.of("turn-1")

        // Act
        val turn = Turn(id = id)

        // Assert
        assertEquals(id, turn.id)
    }
}
