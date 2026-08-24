package com.julian.dixmille.feature.game_setup.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.julian.dixmille.core.domain.model.SavedPlayer
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.reorder_drag_handle_cd
import dixmille.composeapp.generated.resources.reorder_move_down_action
import dixmille.composeapp.generated.resources.reorder_move_up_action
import dixmille.composeapp.generated.resources.reorder_position_announcement
import org.jetbrains.compose.resources.stringResource

/**
 * Fixed row height so drag-distance-to-midpoint math is deterministic (does not depend on
 * measured content size). Must stay in sync with ReorderablePlayerListTest's ROW_HEIGHT_DP.
 */
private val ROW_HEIGHT = 56.dp

/** Elevation applied to the row currently being dragged, to make it visually "lift" off the list. */
private val DRAGGED_ROW_ELEVATION = 6.dp

/** Subtle scale bump applied to the row currently being dragged. */
private const val DRAGGED_ROW_SCALE = 1.03f

@Composable
fun ReorderablePlayerList(
    players: List<SavedPlayer>,
    canReorderPlayers: Boolean,
    onMovePlayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemovePlayer: (playerId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // List-wide: true while ANY row is being dragged, keyed by the stable player id rather
        // than a positional index (an index would go stale the instant a swap reorders the list).
        var draggingPlayerId by remember { mutableStateOf<String?>(null) }

        // Live, continuous drag offset of the currently-dragged row (bounded to roughly
        // ±half a row height between swaps by the swap-threshold math below). Hoisted here so the
        // dragged row's own graphicsLayer can read it every frame and translate with the finger,
        // instead of only jumping when a swap is committed.
        var dragOffsetY by remember { mutableFloatStateOf(0f) }

        val density = LocalDensity.current
        val rowHeightPx = with(density) { ROW_HEIGHT.toPx() }

        players.forEachIndexed { index, player ->
            val haptics = LocalHapticFeedback.current
            val moveUpLabel = stringResource(Res.string.reorder_move_up_action)
            val moveDownLabel = stringResource(Res.string.reorder_move_down_action)
            val dragHandleDescription = stringResource(Res.string.reorder_drag_handle_cd, player.name.value)
            val positionAnnouncement = stringResource(
                Res.string.reorder_position_announcement,
                player.name.value,
                index + 1,
                players.size,
            )
            // Read the latest players list from inside the drag gesture instead of closing over
            // the value captured when the gesture coroutine was launched: onMovePlayer causes the
            // parent to reorder `players` on every threshold crossing, and the gesture must see
            // that new order without itself being cancelled and relaunched (see pointerInput key
            // below).
            val latestPlayers = rememberUpdatedState(players)

            key(player.id.value) {
                val isDragging = draggingPlayerId == player.id.value

                // Manual FLIP-style animation for rows NOT currently being dragged: when a row's
                // index changes because another row swapped past it, snap an offset equal to the
                // distance it just jumped, then animate that offset back to zero so it visually
                // slides into its new slot instead of teleporting.
                val previousIndex = remember { mutableStateOf(index) }
                val slideOffsetY = remember { Animatable(0f) }
                LaunchedEffect(index, isDragging) {
                    val delta = previousIndex.value - index
                    previousIndex.value = index
                    if (!isDragging && delta != 0) {
                        slideOffsetY.snapTo(delta * rowHeightPx)
                        slideOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .padding(vertical = 4.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else slideOffsetY.value
                            shadowElevation = if (isDragging) DRAGGED_ROW_ELEVATION.toPx() else 0f
                            scaleX = if (isDragging) DRAGGED_ROW_SCALE else 1f
                            scaleY = if (isDragging) DRAGGED_ROW_SCALE else 1f
                        }
                        .testTag("player-row-$index")
                        .focusable(interactionSource = remember { MutableInteractionSource() })
                        .semantics {
                            contentDescription = positionAnnouncement
                            liveRegion = LiveRegionMode.Polite
                            customActions = buildList {
                                if (canReorderPlayers) {
                                    if (index > 0) {
                                        add(
                                            CustomAccessibilityAction(label = moveUpLabel) {
                                                onMovePlayer(index, index - 1)
                                                true
                                            },
                                        )
                                    }
                                    if (index < players.lastIndex) {
                                        add(
                                            CustomAccessibilityAction(label = moveDownLabel) {
                                                onMovePlayer(index, index + 1)
                                                true
                                            },
                                        )}
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                    Text(
                        text = player.name.value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    IconButton(
                        onClick = { onRemovePlayer(player.id.value) },
                        enabled = draggingPlayerId == null,
                        modifier = Modifier.testTag("remove-player-$index"),
                    ) {
                        Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (canReorderPlayers) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("drag-handle-$index")
                                .semantics { contentDescription = dragHandleDescription }
                                .pointerInput(player.id.value) {
                                    var dragCurrentIndex = index
                                    var accumulatedOffsetY = 0f
                                    val rowHeightPx = ROW_HEIGHT.toPx()
                                    val halfRowHeightPx = rowHeightPx / 2f

                                    detectDragGestures(
                                        onDragStart = {
                                            dragCurrentIndex = latestPlayers.value.indexOfFirst {
                                                it.id.value == player.id.value
                                            }
                                            accumulatedOffsetY = 0f
                                            dragOffsetY = 0f
                                            draggingPlayerId = player.id.value
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragCancel = {
                                            dragCurrentIndex = latestPlayers.value.indexOfFirst {
                                                it.id.value == player.id.value
                                            }
                                            accumulatedOffsetY = 0f
                                            dragOffsetY = 0f
                                            draggingPlayerId = null
                                        },
                                        onDragEnd = {
                                            dragOffsetY = 0f
                                            draggingPlayerId = null
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedOffsetY += dragAmount.y

                                            val currentPlayers = latestPlayers.value
                                            while (
                                                accumulatedOffsetY > halfRowHeightPx &&
                                                dragCurrentIndex < currentPlayers.lastIndex
                                            ) {
                                                val target = dragCurrentIndex + 1
                                                onMovePlayer(dragCurrentIndex, target)
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragCurrentIndex = target
                                                accumulatedOffsetY -= rowHeightPx
                                            }
                                            while (
                                                accumulatedOffsetY < -halfRowHeightPx &&
                                                dragCurrentIndex > 0
                                            ) {
                                                val target = dragCurrentIndex - 1
                                                onMovePlayer(dragCurrentIndex, target)
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragCurrentIndex = target
                                                accumulatedOffsetY += rowHeightPx
                                            }

                                            if (dragCurrentIndex == 0 && accumulatedOffsetY < 0f) {
                                                accumulatedOffsetY = 0f
                                            }
                                            if (
                                                dragCurrentIndex == currentPlayers.lastIndex &&
                                                accumulatedOffsetY > 0f
                                            ) {
                                                accumulatedOffsetY = 0f
                                            }

                                            dragOffsetY = accumulatedOffsetY
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "☰", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
