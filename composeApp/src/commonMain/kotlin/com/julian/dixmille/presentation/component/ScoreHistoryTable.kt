package com.julian.dixmille.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.TurnOutcome
import com.julian.dixmille.domain.model.TurnRecord

/**
 * Displays a compact score history table showing round-by-round totals.
 * 
 * A round represents a complete cycle where all players have taken one turn.
 * 
 * Shows:
 * - Round numbers in first column
 * - Points per player per round (their turn in that round)
 * - "BUST" indicator for busted turns
 * - "SKIP" indicator for skipped turns
 * - Running totals in bottom row
 */
@Composable
fun ScoreHistoryTable(
    game: Game,
    modifier: Modifier = Modifier
) {
    val players = game.players
    val turnHistory = game.turnHistory
    
    // Group turns by round number
    val turnsByRound = turnHistory.groupBy { it.roundNumber }
    val maxRound = turnsByRound.keys.maxOrNull() ?: 0
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp)
        ) {
            // Round number column
            Text(
                text = "Round",
                modifier = Modifier.width(50.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Player columns
            players.forEach { player ->
                Text(
                    text = player.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }
        }
        
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        
        // Scrollable round rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        ) {
            for (roundNum in 1..maxRound) {
                val turnsForThisRound = turnsByRound[roundNum] ?: emptyList()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Round number
                    Text(
                        text = roundNum.toString(),
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Player scores for this round
                    players.forEach { player ->
                        val playerTurn = turnsForThisRound.filter { it.playerId == player.id }.lastOrNull()
                        
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                playerTurn == null -> {
                                    // Player hasn't played this round yet
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                playerTurn.outcome == TurnOutcome.BUST -> {
                                    // Player busted
                                    Text(
                                        text = "BUST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                playerTurn.outcome == TurnOutcome.SKIP -> {
                                    // Player skipped
                                    Text(
                                        text = "SKIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                playerTurn.outcome == TurnOutcome.COLLISION -> {
                                    // Player was hit by collision
                                    Text(
                                        text = "HIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    // Show points (SCORED)
                                    Text(
                                        text = "+${playerTurn.points}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (roundNum < maxRound) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
        
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        
        // Total row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp)
        ) {
            // "TOTAL" label
            Text(
                text = "TOTAL",
                modifier = Modifier.width(50.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            // Player totals
            players.forEach { player ->
                Text(
                    text = player.totalScore.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
