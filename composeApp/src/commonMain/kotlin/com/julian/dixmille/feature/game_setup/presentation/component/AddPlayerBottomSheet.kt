package com.julian.dixmille.feature.game_setup.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julian.dixmille.feature.game_setup.presentation.model.GameSetupEvent
import com.julian.dixmille.feature.game_setup.presentation.model.GameSetupUiState
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.add_player_add_button
import dixmille.composeapp.generated.resources.add_player_confirm_selection_cd
import dixmille.composeapp.generated.resources.add_player_existing_players_header
import dixmille.composeapp.generated.resources.add_player_sheet_close_cd
import dixmille.composeapp.generated.resources.add_player_sheet_title
import dixmille.composeapp.generated.resources.add_player_subtitle_already_in_game
import dixmille.composeapp.generated.resources.add_player_subtitle_available
import dixmille.composeapp.generated.resources.add_player_subtitle_days_ago
import dixmille.composeapp.generated.resources.add_player_subtitle_one_day_ago
import dixmille.composeapp.generated.resources.add_player_subtitle_today
import dixmille.composeapp.generated.resources.add_player_unified_field_hint
import dixmille.composeapp.generated.resources.play_arrow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerBottomSheet(
    state: GameSetupUiState,
    onEvent: (GameSetupEvent) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = 0L,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = { onEvent(GameSetupEvent.HidePlayerSelector) },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onEvent(GameSetupEvent.HidePlayerSelector) }) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.add_player_sheet_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.unifiedInput,
                        onValueChange = { onEvent(GameSetupEvent.UpdateUnifiedInput(it)) },
                        label = { Text(stringResource(Res.string.add_player_unified_field_hint)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = state.quickAddError != null,
                        trailingIcon = if (state.unifiedInput.isNotEmpty()) {
                            {
                                IconButton(onClick = { onEvent(GameSetupEvent.UpdateUnifiedInput("")) }) {
                                    Text(text = "✕", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    )
                    Button(
                        onClick = { onEvent(GameSetupEvent.QuickAddPlayer(state.unifiedInput)) },
                        enabled = state.canAddNewPlayer,
                    ) {
                        Text(stringResource(Res.string.add_player_add_button))
                    }
                }

                state.quickAddError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text = stringResource(Res.string.add_player_existing_players_header),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.filteredPlayers, key = { it.id.value }) { player ->
                        val isSelected = state.selectedPlayers.any { it.id.value == player.id.value }
                        val subtitle = when {
                            isSelected -> stringResource(Res.string.add_player_subtitle_already_in_game)
                            player.lastPlayedAt != null -> {
                                val days = abs(nowMillis - player.lastPlayedAt) / MILLIS_PER_DAY
                                when {
                                    days == 0L -> stringResource(Res.string.add_player_subtitle_today)
                                    days == 1L -> stringResource(Res.string.add_player_subtitle_one_day_ago)
                                    else -> stringResource(Res.string.add_player_subtitle_days_ago, days)
                                }
                            }
                            else -> stringResource(Res.string.add_player_subtitle_available)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = player.name.value.first().uppercaseChar().toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = player.name.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onEvent(GameSetupEvent.SelectPlayer(player))
                                    } else {
                                        onEvent(GameSetupEvent.DeselectPlayer(player.id.value))
                                    }
                                },
                                enabled = isSelected || state.canAddMorePlayers,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            FloatingActionButton(
                onClick = {
                    if (state.canConfirmSelection) {
                        onEvent(GameSetupEvent.ConfirmPlayerSelection(state.selectedPlayers))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.play_arrow),
                    contentDescription = stringResource(Res.string.add_player_confirm_selection_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
