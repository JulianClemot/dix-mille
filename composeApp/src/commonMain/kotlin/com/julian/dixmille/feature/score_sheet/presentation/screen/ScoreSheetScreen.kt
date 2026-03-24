package com.julian.dixmille.feature.score_sheet.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.core.presentation.navigation.ScoreSheetNavigationEvent
import com.julian.dixmille.feature.game_end.presentation.screen.GameEndRoute
import com.julian.dixmille.feature.score_sheet.presentation.component.CustomScoreInput
import com.julian.dixmille.feature.score_sheet.presentation.component.PlayerScoreCard
import com.julian.dixmille.feature.score_sheet.presentation.component.PresetScoreButtons
import com.julian.dixmille.feature.score_sheet.presentation.component.ScoreHistoryTable
import com.julian.dixmille.feature.score_sheet.presentation.model.ScoreSheetEvent
import com.julian.dixmille.feature.score_sheet.presentation.model.ScoreSheetUiState
import com.julian.dixmille.feature.score_sheet.presentation.viewmodel.ScoreSheetViewModel
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.arrow_back
import dixmille.composeapp.generated.resources.bomb
import dixmille.composeapp.generated.resources.score_sheet_bust_button
import dixmille.composeapp.generated.resources.score_sheet_entry_warning
import dixmille.composeapp.generated.resources.score_sheet_final_round_banner
import dixmille.composeapp.generated.resources.score_sheet_round
import dixmille.composeapp.generated.resources.score_sheet_skip_button
import dixmille.composeapp.generated.resources.score_sheet_title
import dixmille.composeapp.generated.resources.score_sheet_undo_button
import dixmille.composeapp.generated.resources.skip_next
import dixmille.composeapp.generated.resources.undo
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Custom top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_back),
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.score_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(Res.string.score_sheet_round, game.roundNumber),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                }

                // Spacer to balance the back button
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Final round banner
        if (state.isFinalRound) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.score_sheet_final_round_banner),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Player cards row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(game.players, key = { it.id }) { player ->
                PlayerScoreCard(
                    player = player,
                    isCurrentPlayer = player.id == state.currentPlayer?.id
                )
            }
        }

        // Entry warning under player cards
        state.currentPlayer?.let { currentPlayer ->
            if (!currentPlayer.hasEnteredGame && state.currentTurnTotal > 0 && state.currentTurnTotal < 500) {
                Text(
                    text = stringResource(
                        Res.string.score_sheet_entry_warning,
                        500 - state.currentTurnTotal
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // 3. Recent Rounds card (fills remaining space)
        ScoreHistoryTable(
            game = game,
            scrollState = scrollState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // 4. Bottom controls area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Custom score input
            CustomScoreInput(
                onScoreSubmit = { points ->
                    onEvent(ScoreSheetEvent.AddScore(points, isPreset = false))
                }
            )

            // Preset score buttons (4x4 grid)
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

            // 5. Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Undo button
                OutlinedButton(
                    onClick = { onEvent(ScoreSheetEvent.UndoLastTurn) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    enabled = state.canUndoTurn
                ) {
                    Icon(painter = painterResource(Res.drawable.undo), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.score_sheet_undo_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp),
                        color = if (state.canUndoTurn) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                }

                // Skip button
                OutlinedButton(
                    onClick = { onEvent(ScoreSheetEvent.SkipTurn) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_next),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.score_sheet_skip_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bust button
                OutlinedButton(
                    onClick = { onEvent(ScoreSheetEvent.BustTurn) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.bomb),
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.score_sheet_bust_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
