package com.julian.dixmille.presentation.screen

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.presentation.model.GameRulesSettingsEvent
import com.julian.dixmille.presentation.model.GameRulesSettingsUiState
import com.julian.dixmille.presentation.navigation.GameRulesSettingsNavigationEvent
import com.julian.dixmille.presentation.viewmodel.GameRulesSettingsViewModel
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.arrow_back
import dixmille.composeapp.generated.resources.restart_alt
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object GameRulesSettingsRoute : NavKey

@Composable
fun GameRulesSettingsEntryPoint(
    viewModel: GameRulesSettingsViewModel = koinViewModel(),
    backStack: NavBackStack<NavKey>,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is GameRulesSettingsNavigationEvent.NavigateBack -> {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    GameRulesSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun GameRulesSettingsContent(
    state: GameRulesSettingsUiState,
    onEvent: (GameRulesSettingsEvent) -> Unit,
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

    // Discard dialog
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(GameRulesSettingsEvent.DismissDiscardDialog) },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = { onEvent(GameRulesSettingsEvent.ConfirmDiscard) }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(GameRulesSettingsEvent.DismissDiscardDialog) }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
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
                IconButton(onClick = { onEvent(GameRulesSettingsEvent.NavigateBack) }) {
                    Icon(painter = painterResource(Res.drawable.arrow_back), contentDescription = null)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME RULES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                }

                // Spacer to balance the back button
                Spacer(modifier = Modifier.size(48.dp))
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

            // Scoring section
            SectionHeader("SCORING")

            NumericRuleField(
                label = "Target Score",
                value = state.targetScore,
                defaultValue = GameRules.DEFAULT_TARGET_SCORE.toString(),
                onValueChange = { onEvent(GameRulesSettingsEvent.UpdateTargetScore(it)) },
                onReset = { onEvent(GameRulesSettingsEvent.ResetTargetScore) },
                colors = textFieldColors
            )

            NumericRuleField(
                label = "Entry Minimum Score",
                value = state.entryMinimumScore,
                defaultValue = GameRules.DEFAULT_ENTRY_MINIMUM_SCORE.toString(),
                onValueChange = { onEvent(GameRulesSettingsEvent.UpdateEntryMinimumScore(it)) },
                onReset = { onEvent(GameRulesSettingsEvent.ResetEntryMinimumScore) },
                colors = textFieldColors
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Players section
            SectionHeader("PLAYERS")

            NumericRuleField(
                label = "Min Players",
                value = state.minPlayers,
                defaultValue = GameRules.DEFAULT_MIN_PLAYERS.toString(),
                onValueChange = { onEvent(GameRulesSettingsEvent.UpdateMinPlayers(it)) },
                onReset = { onEvent(GameRulesSettingsEvent.ResetMinPlayers) },
                colors = textFieldColors
            )

            NumericRuleField(
                label = "Max Players",
                value = state.maxPlayers,
                defaultValue = GameRules.DEFAULT_MAX_PLAYERS.toString(),
                onValueChange = { onEvent(GameRulesSettingsEvent.UpdateMaxPlayers(it)) },
                onReset = { onEvent(GameRulesSettingsEvent.ResetMaxPlayers) },
                colors = textFieldColors
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Penalties section
            SectionHeader("PENALTIES")

            BooleanRuleField(
                label = "Bust Penalty",
                description = "Revert score after consecutive busts",
                checked = state.enableBustPenalty,
                onCheckedChange = { onEvent(GameRulesSettingsEvent.UpdateEnableBustPenalty(it)) }
            )

            if (state.enableBustPenalty) {
                NumericRuleField(
                    label = "Consecutive Busts for Penalty",
                    value = state.consecutiveBustsForPenalty,
                    defaultValue = GameRules.DEFAULT_CONSECUTIVE_BUSTS_FOR_PENALTY.toString(),
                    onValueChange = { onEvent(GameRulesSettingsEvent.UpdateConsecutiveBustsForPenalty(it)) },
                    onReset = { onEvent(GameRulesSettingsEvent.ResetConsecutiveBustsForPenalty) },
                    colors = textFieldColors
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Game flow section
            SectionHeader("GAME FLOW")

            BooleanRuleField(
                label = "Final Round",
                description = "Other players get one more turn after target is reached",
                checked = state.enableFinalRound,
                onCheckedChange = { onEvent(GameRulesSettingsEvent.UpdateEnableFinalRound(it)) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Reset all
            TextButton(
                onClick = { onEvent(GameRulesSettingsEvent.ResetAll) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(Res.drawable.restart_alt),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESET ALL TO DEFAULTS")
            }

            // Error
            state.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { onEvent(GameRulesSettingsEvent.Save) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "SAVE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = 1.sp
    )
}

@Composable
private fun NumericRuleField(
    label: String,
    value: String,
    defaultValue: String,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = colors
        )

        if (value != defaultValue) {
            IconButton(onClick = onReset) {
                Icon(
                    painter = painterResource(Res.drawable.restart_alt),
                    contentDescription = "Reset to default",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun BooleanRuleField(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
