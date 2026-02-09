package com.julian.dixmille.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julian.dixmille.domain.model.PresetScores

/**
 * Grid of preset score buttons for quick entry.
 */
@Composable
fun PresetScoreButtons(
    onScoreClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onScoreClick(50, PresetScores.ONE_5.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("50")
            }
            FilledTonalButton(
                onClick = { onScoreClick(100, PresetScores.ONE_1.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("100")
            }
            FilledTonalButton(
                onClick = { onScoreClick(150, PresetScores.ONE_1_ONE_5.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("150")
            }
        }
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onScoreClick(200, PresetScores.TWO_1S_OR_THREE_2S.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("200")
            }
            FilledTonalButton(
                onClick = { onScoreClick(250, PresetScores.TWO_1S_ONE_5.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("250")
            }
            FilledTonalButton(
                onClick = { onScoreClick(300, PresetScores.THREE_1S_OR_THREE_3S.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("300")
            }
        }
        
        // Third row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onScoreClick(400, PresetScores.FOUR_1S_OR_THREE_4S.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("400")
            }
            FilledTonalButton(
                onClick = { onScoreClick(500, PresetScores.FIVE_1S_OR_THREE_5S.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("500")
            }
            FilledTonalButton(
                onClick = { onScoreClick(600, PresetScores.SIX_1S_OR_THREE_6S.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("600")
            }
        }
        
        // Fourth row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onScoreClick(1000, PresetScores.THREE_1S_FIRST_ROLL.label) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("1000")
            }
            // Custom button removed - now inline input
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
