package com.julian.dixmille.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.presentation.model.GameSetupEvent
import com.julian.dixmille.presentation.model.GameSetupUiState
import com.julian.dixmille.presentation.navigation.GameSetupNavigationEvent
import com.julian.dixmille.presentation.viewmodel.GameSetupViewModel
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.arrow_back
import dixmille.composeapp.generated.resources.person_add
import dixmille.composeapp.generated.resources.play_arrow
import dixmille.composeapp.generated.resources.settings
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
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
    onShowSnackbar: (message: String?) -> Unit,
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

                is GameSetupNavigationEvent.NavigateToRulesSettings -> {
                    backStack += GameRulesSettingsRoute
                }
            }
        }
    }

    // Refresh rules when returning from settings (backStack size changes)
    LaunchedEffect(backStack.size) {
        viewModel.refreshRules()
    }

    // Handle errors via snackbar
    LaunchedEffect(state.error) {
        onShowSnackbar(state.error)
    }

    GameSetupContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = viewModel::navigateBack,
        onNavigateToRulesSettings = viewModel::navigateToRulesSettings,
    )
}

/**
 * Game Setup screen Content - pure UI composable.
 */
@Composable
fun GameSetupContent(
    state: GameSetupUiState,
    onEvent: (GameSetupEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRulesSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom top bar (matching ScoreSheet style)
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
                    Icon(painter = painterResource(Res.drawable.arrow_back), contentDescription = null)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NEW GAME",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                }

                IconButton(onClick = onNavigateToRulesSettings) {
                    Icon(painter = painterResource(Res.drawable.settings), contentDescription = null)
                }
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Players section header
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PLAYERS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "(${state.minPlayers}-${state.maxPlayers})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            // Player input cards
            state.playerNames.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            onEvent(GameSetupEvent.UpdatePlayerName(index, newName))
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = if(index < state.playerNames.size - 1) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        label = { Text("Player ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = textFieldColors
                    )

                    if (state.playerNames.size > state.minPlayers) {
                        TextButton(
                            onClick = { onEvent(GameSetupEvent.RemovePlayer(index)) }
                        ) {
                            Text(
                                text = "\u2715",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Add Player button
            if (state.playerNames.size < state.maxPlayers) {
                OutlinedButton(
                    onClick = { onEvent(GameSetupEvent.AddPlayer) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.person_add),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD PLAYER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Target Score section header
            Text(
                text = "TARGET SCORE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = state.targetScore,
                onValueChange = { onEvent(GameSetupEvent.UpdateTargetScore(it)) },
                label = { Text("Target Score") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            state.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Game button
            Button(
                onClick = { onEvent(GameSetupEvent.CreateGame) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isCreating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(painter = painterResource(Res.drawable.play_arrow), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START GAME",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
