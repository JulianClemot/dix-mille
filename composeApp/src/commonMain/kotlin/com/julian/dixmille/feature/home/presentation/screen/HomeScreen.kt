package com.julian.dixmille.feature.home.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.core.presentation.navigation.HomeNavigationEvent
import com.julian.dixmille.feature.game_setup.presentation.screen.GameSetupRoute
import com.julian.dixmille.feature.home.presentation.model.HomeEvent
import com.julian.dixmille.feature.home.presentation.model.HomeUiState
import com.julian.dixmille.feature.home.presentation.viewmodel.HomeViewModel
import com.julian.dixmille.feature.score_sheet.presentation.screen.ScoreSheetRoute
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.add_diamond
import dixmille.composeapp.generated.resources.casino
import dixmille.composeapp.generated.resources.home_game_in_progress
import dixmille.composeapp.generated.resources.home_new_game_button
import dixmille.composeapp.generated.resources.home_new_game_warning
import dixmille.composeapp.generated.resources.home_resume_game_button
import dixmille.composeapp.generated.resources.home_subtitle
import dixmille.composeapp.generated.resources.home_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Home screen route - landing page with New Game and Resume options.
 */
@Serializable
data object HomeRoute : NavKey

/**
 * Home screen EntryPoint - handles ViewModel injection and state collection.
 */
@Composable
fun HomeEntryPoint(viewModel: HomeViewModel = koinViewModel(), backStack: NavBackStack<NavKey>) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Refresh game status each time this screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshGameStatus()
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            backStack.add(
                when (event) {
                    is HomeNavigationEvent.NavigateToGameSetup -> {
                        GameSetupRoute
                    }

                    is HomeNavigationEvent.NavigateToScoreSheet -> {
                        ScoreSheetRoute
                    }
                }
            )
        }
    }

    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

/**
 * Home screen Content - pure UI composable.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = stringResource(Res.string.home_title),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle
        Text(
            text = stringResource(Res.string.home_subtitle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Game in progress card
        if (state.hasExistingGame && state.gameStatusSummary != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.home_game_in_progress),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.gameStatusSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Resume Game button
            Button(
                onClick = { onEvent(HomeEvent.NavigateToResumeGame) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(painter = painterResource(Res.drawable.casino), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.home_resume_game_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // New Game button
        if (state.hasExistingGame) {
            OutlinedButton(
                onClick = { onEvent(HomeEvent.NavigateToNewGame) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(painter = painterResource(Res.drawable.add_diamond), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.home_new_game_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Button(
                onClick = { onEvent(HomeEvent.NavigateToNewGame) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(painter = painterResource(Res.drawable.add_diamond), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.home_new_game_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        if (state.hasExistingGame) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_new_game_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
