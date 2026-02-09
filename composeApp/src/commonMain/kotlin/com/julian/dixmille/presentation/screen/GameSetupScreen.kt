package com.julian.dixmille.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.presentation.model.GameSetupEvent
import com.julian.dixmille.presentation.model.GameSetupUiState
import com.julian.dixmille.presentation.navigation.GameSetupNavigationEvent
import com.julian.dixmille.presentation.viewmodel.GameSetupViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * Game setup screen route - configure new game with players and target score.
 */
@Serializable
data object GameSetupRoute : NavKey


/**
 * Game Setup screen EntryPoint - handles ViewModel injection and state collection.
 */
@Composable
fun GameSetupEntryPoint(
    viewModel: GameSetupViewModel = koinViewModel(),
    backStack: NavBackStack<NavKey>,
    onShowSnackbar : (message : String?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is GameSetupNavigationEvent.NavigateToScoreSheet -> {
                    backStack.removeLastOrNull() // Remove setup screen
                    backStack += ScoreSheetRoute
                }

                is GameSetupNavigationEvent.NavigateBack -> {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    // Handle errors via snackbar
    LaunchedEffect(state.error) {
        onShowSnackbar(state.error)
    }

    GameSetupContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

/**
 * Game Setup screen Content - pure UI composable.
 *
 * @param state UI state from GameSetupViewModel
 * @param onEvent Event handler for user actions
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupContent(
    state: GameSetupUiState,
    onEvent: (GameSetupEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // TopAppBar with back button
        TopAppBar(
            title = { Text("New Game Setup") },
            navigationIcon = {
                IconButton(onClick = { onEvent(GameSetupEvent.NavigateBack) }) {
                    Text("◀", style = MaterialTheme.typography.titleLarge)
                }
            }
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Players (2-6)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            state.playerNames.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            onEvent(GameSetupEvent.UpdatePlayerName(index, newName))
                        },
                        label = { Text("Player ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    if (state.playerNames.size > 2) {
                        TextButton(
                            onClick = { onEvent(GameSetupEvent.RemovePlayer(index)) }
                        ) {
                            Text("✕")
                        }
                    }
                }
            }

            if (state.playerNames.size < 6) {
                OutlinedButton(
                    onClick = { onEvent(GameSetupEvent.AddPlayer) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ Add Player")
                }
            }

            HorizontalDivider()

            Text(
                text = "Target Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = state.targetScore,
                onValueChange = { onEvent(GameSetupEvent.UpdateTargetScore(it)) },
                label = { Text("Target Score") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            state.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onEvent(GameSetupEvent.CreateGame) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreating
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Start Game")
                }
            }
        }
    }
}
