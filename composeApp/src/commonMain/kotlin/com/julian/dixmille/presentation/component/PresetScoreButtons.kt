package com.julian.dixmille.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julian.dixmille.domain.model.PresetScores

/**
 * 4x4 grid of preset score buttons with outlined dark style.
 */
@Composable
fun PresetScoreButtons(
    onScoreClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(PresetScores.ONE_5, PresetScores.ONE_1, PresetScores.ONE_1_ONE_5, PresetScores.TWO_1S_OR_THREE_2S),
        listOf(PresetScores.TWO_1S_ONE_5, PresetScores.THREE_1S_OR_THREE_3S, PresetScores.THREE_3S_ONE_5, PresetScores.FOUR_1S_OR_THREE_4S),
        listOf(PresetScores.THREE_4S_ONE_5, PresetScores.FIVE_1S_OR_THREE_5S, PresetScores.SIX_1S_OR_THREE_6S, PresetScores.THREE_5S_TWO_1S),
        listOf(PresetScores.THREE_1S_FIRST_ROLL, PresetScores.FOUR_1S_FIRST_ROLL, PresetScores.FIVE_1S_FIRST_ROLL, PresetScores.SIX_1S_FIRST_ROLL)
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { preset ->
                    ScoreButton(
                        text = preset.points.toString(),
                        onClick = { onScoreClick(preset.points, preset.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
