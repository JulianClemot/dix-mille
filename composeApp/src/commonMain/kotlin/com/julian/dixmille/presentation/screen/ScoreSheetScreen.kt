package com.julian.dixmille.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.presentation.component.*
import com.julian.dixmille.presentation.model.ScoreSheetEvent
import com.julian.dixmille.presentation.model.ScoreSheetUiState
import com.julian.dixmille.presentation.navigation.ScoreSheetNavigationEvent
import com.julian.dixmille.presentation.viewmodel.ScoreSheetViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * Score sheet screen route - main game screen with score tracking.
 */
@Serializable
data object ScoreSheetRoute : NavKey

/**
 * Score Sheet screen EntryPoint - handles ViewModel injection and state collection.
 */
@Composable
fun ScoreSheetEntryPoint(
    viewModel: ScoreSheetViewModel = koinViewModel(), backStack: NavBackStack<NavKey>,
    onShowSnackbar: (message: String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reload game each time this screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshGame()
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ScoreSheetNavigationEvent.NavigateToGameEnd -> {
                    backStack.removeLastOrNull()
                    backStack += GameEndRoute(
                        winnerName = event.winnerName,
                        winnerScore = event.winnerScore
                    )
                }

                is ScoreSheetNavigationEvent.NavigateBack -> {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    // Handle errors via snackbar
    LaunchedEffect(state.error) {
        onShowSnackbar(state.error)
    }

    ScoreSheetContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = viewModel::navigateBack,
    )
}

/**
 * Score Sheet screen Content - pure UI composable.
 *
 * Features:
 * - Score history table at top
 * - Sticky bottom bar with controls
 * - Auto-commit on valid score entry
 * - Back button to return to home (auto-saves)
 *
 * @param state UI state from ScoreSheetViewModel
 * @param onEvent Event handler for user actions
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreSheetContent(
    state: ScoreSheetUiState,
    onEvent: (ScoreSheetEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val game = state.game ?: return
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when turn history changes
    LaunchedEffect(state.game.turnHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // TopAppBar with back button
        CenterAlignedTopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dix Mille")
                    Text(
                        text = "Turn ${game.roundNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Text("◀", style = MaterialTheme.typography.titleLarge)
                }
            },
            windowInsets = WindowInsets(0.dp),
        )

        // Sticky: Final round banner
        if (state.isFinalRound) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "🏁 FINAL ROUND - Last chance for all players!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Sticky: Current player banner
        state.currentPlayer?.let { currentPlayer ->
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${currentPlayer.name}'s Turn",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (state.currentTurnTotal > 0) {
                        Text(
                            text = "Current turn: ${state.currentTurnTotal} points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Entry warning
                    if (!currentPlayer.hasEnteredGame && state.currentTurnTotal > 0 && state.currentTurnTotal < 500) {
                        Text(
                            text = "⚠️ Need ${500 - state.currentTurnTotal} more points to enter",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Scrollable: Score history table (fills remaining space)
        ScoreHistoryTable(
            game = game,
            scrollState = scrollState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Sticky bottom bar with controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Custom score input (inline)
                CustomScoreInput(
                    onScoreSubmit = { points ->
                        onEvent(ScoreSheetEvent.AddScore(points, isPreset = false))
                    }
                )

                // Preset score buttons
                PresetScoreButtons(
                    onScoreClick = { points, label ->
                        onEvent(
                            ScoreSheetEvent.AddScore(
                                points,
                                isPreset = true,
                                label = label
                            )
                        )
                    }
                )

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Undo Turn button
                    OutlinedButton(
                        onClick = { onEvent(ScoreSheetEvent.UndoLastTurn) },
                        modifier = Modifier.weight(1f),
                        enabled = state.canUndoTurn,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔄 Undo")
                    }

                    // Skip button
                    FilledTonalButton(
                        onClick = { onEvent(ScoreSheetEvent.SkipTurn) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⏭️ Skip")
                    }

                    // Bust button
                    FilledTonalButton(
                        onClick = { onEvent(ScoreSheetEvent.BustTurn) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💥 Bust")
                    }
                }
            }
        }
    }
}
