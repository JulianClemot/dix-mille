package com.julian.dixmille.feature.game_setup.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.julian.dixmille.core.domain.model.SavedPlayer
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import dixmille.composeapp.generated.resources.Res
import dixmille.composeapp.generated.resources.reorder_move_down_action
import dixmille.composeapp.generated.resources.reorder_position_announcement
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ReorderablePlayerList renders each row at a fixed height of [ROW_HEIGHT_DP] (must match the
 * production composable's internal row height constant) so that drag-distance-to-midpoint math
 * here is deterministic instead of depending on measured content size.
 *
 * Lives in androidDeviceTest (not commonTest): `createAndroidComposeRule`/JUnit4 `@Rule`-based
 * Compose UI testing is Android-instrumented-only (not part of the Kotlin Multiplatform common
 * test API), and runs against a real Android runtime on a connected device/emulator via
 * `connectedAndroidTest`/`androidConnectedCheck` — no shadowing, no Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class ReorderablePlayerListTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val ROW_HEIGHT_DP = 56

    private val alice = SavedPlayer(PlayerId("id-alice"), PlayerName("Alice"), 0L, null)
    private val bob = SavedPlayer(PlayerId("id-bob"), PlayerName("Bob"), 0L, null)
    private val carol = SavedPlayer(PlayerId("id-carol"), PlayerName("Carol"), 0L, null)
    private val dave = SavedPlayer(PlayerId("id-dave"), PlayerName("Dave"), 0L, null)

    @Test
    fun Should_invoke_onMovePlayer_with_adjacent_indices_when_dragging_a_row_down_past_the_next_rows_midpoint() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        // The first moveTo of a gesture must overshoot the intended distance by touchSlopPx:
        // detectDragGestures consumes that much movement internally before onDrag starts firing.
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            up()
        }

        // Assert
        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun Should_invoke_onMovePlayer_with_adjacent_indices_when_dragging_a_row_up_past_the_previous_rows_midpoint() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-2").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, -(touchSlopPx + rowHeightPx * 0.6f)))
            up()
        }

        // Assert
        assertEquals(listOf(2 to 1), moves)
    }

    @Test
    fun Should_invoke_onMovePlayer_once_per_midpoint_crossing_when_dragging_across_multiple_rows() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol, dave),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        // Slop is only consumed once, at the very start of the gesture, so only the first
        // moveTo's target needs the touchSlopPx offset; the second moveTo's delta from the
        // first is already a "real" (post-slop) drag delta.
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 1.6f))
            up()
        }

        // Assert
        assertEquals(listOf(0 to 1, 1 to 2), moves)
    }

    @Test
    fun Should_invoke_onMovePlayer_sequence_moving_last_row_to_first_position() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-2").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, -(touchSlopPx + rowHeightPx * 0.6f)))
            moveTo(center + Offset(0f, -(touchSlopPx + rowHeightPx * 1.6f)))
            up()
        }

        // Assert
        assertEquals(listOf(2 to 1, 1 to 0), moves)
    }

    @Test
    fun Should_not_invoke_onMovePlayer_when_drag_released_before_crossing_a_midpoint() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, rowHeightPx * 0.3f))
            up()
        }

        // Assert
        assertTrue(moves.isEmpty())
    }

    @Test
    fun Should_not_invoke_onMovePlayer_with_out_of_range_index_when_dragging_past_the_top_of_the_list() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, -rowHeightPx * 2f))
            up()
        }

        // Assert
        assertTrue(moves.isEmpty())
    }

    @Test
    fun Should_not_invoke_onMovePlayer_with_out_of_range_index_when_dragging_past_the_bottom_of_the_list() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-2").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, rowHeightPx * 2f))
            up()
        }

        // Assert
        assertTrue(moves.isEmpty())
    }

    @Test
    fun Should_not_invoke_additional_onMovePlayer_calls_when_drag_gesture_is_cancelled_mid_drag() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            cancel()
        }

        // Assert
        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun Should_not_render_drag_handle_when_canReorderPlayers_is_false() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice),
                canReorderPlayers = false,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act & Assert
        composeTestRule.onNodeWithTag("drag-handle-0").assertDoesNotExist()
    }

    @Test
    fun Should_expose_move_up_and_move_down_actions_for_middle_row() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val node = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()

        // Assert
        assertEquals(listOf("Move up", "Move down"), actions.map { it.label })
    }

    @Test
    fun Should_expose_only_move_down_action_for_first_row() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val node = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()

        // Assert
        assertEquals(listOf("Move down"), actions.map { it.label })
    }

    @Test
    fun Should_expose_only_move_up_action_for_last_row() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val node = composeTestRule.onNodeWithTag("player-row-2").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()

        // Assert
        assertEquals(listOf("Move up"), actions.map { it.label })
    }

    @Test
    fun Should_expose_no_move_actions_when_canReorderPlayers_is_false() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice),
                canReorderPlayers = false,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val node = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()

        // Assert
        assertTrue(actions.isEmpty())
    }

    @Test
    fun Should_invoke_onMovePlayer_with_adjacent_indices_when_move_up_action_performed() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val node = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()
        val moveUp = actions.first { it.label == "Move up" }

        // Act
        moveUp.action()

        // Assert
        assertEquals(listOf(1 to 0), moves)
    }

    @Test
    fun Should_invoke_onMovePlayer_with_adjacent_indices_when_move_down_action_performed() {
        // Arrange
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { from, to -> moves.add(from to to) },
                onRemovePlayer = {},
            )
        }
        val node = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()
        val moveDown = actions.first { it.label == "Move down" }

        // Act
        moveDown.action()

        // Assert
        assertEquals(listOf(1 to 2), moves)
    }

    @Test
    fun Should_trigger_haptic_feedback_once_when_drag_starts() {
        // Arrange
        val fakeHaptics = object : HapticFeedback {
            val calls = mutableListOf<HapticFeedbackType>()
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                calls.add(hapticFeedbackType)
            }
        }
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            CompositionLocalProvider(LocalHapticFeedback provides fakeHaptics) {
                ReorderablePlayerList(
                    players = listOf(alice, bob, carol),
                    canReorderPlayers = true,
                    onMovePlayer = { _, _ -> },
                    onRemovePlayer = {},
                )
            }
        }

        // Act
        // onDragStart only fires once the pointer clears touch slop, not on a raw down() —
        // same touch-slop accounting as the drag-distance tests above.
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + 1f))
        }

        // Assert
        assertEquals(1, fakeHaptics.calls.size)

        // Cleanup: release the still-open partial gesture so the test doesn't leak state.
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput { up() }
    }

    @Test
    fun Should_trigger_haptic_feedback_once_per_midpoint_crossing_during_drag() {
        // Arrange
        val fakeHaptics = object : HapticFeedback {
            val calls = mutableListOf<HapticFeedbackType>()
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                calls.add(hapticFeedbackType)
            }
        }
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            CompositionLocalProvider(LocalHapticFeedback provides fakeHaptics) {
                ReorderablePlayerList(
                    players = listOf(alice, bob, carol, dave),
                    canReorderPlayers = true,
                    onMovePlayer = { _, _ -> },
                    onRemovePlayer = {},
                )
            }
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 1.6f))
            up()
        }

        // Assert: 1 lift + 2 swaps + 1 drop (the gesture ends with up(), which also fires the
        // drop haptic — see Should_trigger_haptic_feedback_once_on_drag_release for the same
        // "+1 drop on up()" accounting with a single crossing).
        assertEquals(4, fakeHaptics.calls.size)
    }

    @Test
    fun Should_trigger_haptic_feedback_once_on_drag_release() {
        // Arrange
        val fakeHaptics = object : HapticFeedback {
            val calls = mutableListOf<HapticFeedbackType>()
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                calls.add(hapticFeedbackType)
            }
        }
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            CompositionLocalProvider(LocalHapticFeedback provides fakeHaptics) {
                ReorderablePlayerList(
                    players = listOf(alice, bob, carol),
                    canReorderPlayers = true,
                    onMovePlayer = { _, _ -> },
                    onRemovePlayer = {},
                )
            }
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            up()
        }

        // Assert: 1 lift + 1 swap + 1 drop
        assertEquals(3, fakeHaptics.calls.size)
    }

    @Test
    fun Should_not_trigger_drop_haptic_feedback_when_drag_is_cancelled() {
        // Arrange
        val fakeHaptics = object : HapticFeedback {
            val calls = mutableListOf<HapticFeedbackType>()
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                calls.add(hapticFeedbackType)
            }
        }
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            CompositionLocalProvider(LocalHapticFeedback provides fakeHaptics) {
                ReorderablePlayerList(
                    players = listOf(alice, bob, carol),
                    canReorderPlayers = true,
                    onMovePlayer = { _, _ -> },
                    onRemovePlayer = {},
                )
            }
        }
        val rowHeightPx = with(composeTestRule.density) { ROW_HEIGHT_DP.dp.toPx() }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + rowHeightPx * 0.6f))
            cancel()
        }

        // Assert: 1 lift + 1 swap, no drop
        assertEquals(2, fakeHaptics.calls.size)
    }

    @Test
    fun Should_include_player_name_in_drag_handle_content_description() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val node = composeTestRule.onNodeWithTag("drag-handle-0").fetchSemanticsNode()
        val contentDescription = node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

        // Assert
        assertNotNull(contentDescription)
        assertTrue(contentDescription!!.contains("Alice"))
    }

    @Test
    fun Should_have_distinct_drag_handle_descriptions_per_row() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act
        val description0 = composeTestRule.onNodeWithTag("drag-handle-0").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        val description1 = composeTestRule.onNodeWithTag("drag-handle-1").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        val description2 = composeTestRule.onNodeWithTag("drag-handle-2").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

        // Assert
        val descriptions = setOf(description0, description1, description2)
        assertEquals(3, descriptions.size)
    }

    @Test
    fun Should_expose_position_announcement_text_for_middle_row() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }
        val expected = runBlocking {
            getString(Res.string.reorder_position_announcement, "Bob", 2, 3)
        }

        // Act
        val node = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
        val contentDescription = node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

        // Assert
        assertEquals(expected, contentDescription)
    }

    @Test
    fun Should_update_position_announcement_after_reorder() {
        // Arrange
        var playerList by mutableStateOf(listOf(alice, bob, carol))
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = playerList,
                canReorderPlayers = true,
                onMovePlayer = { from, to ->
                    playerList = playerList.toMutableList().apply { add(to, removeAt(from)) }
                },
                onRemovePlayer = {},
            )
        }
        val moveDownLabel = runBlocking { getString(Res.string.reorder_move_down_action) }
        val expected = runBlocking {
            getString(Res.string.reorder_position_announcement, "Alice", 2, 3)
        }
        val node = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()
        val moveDown = actions.first { it.label == moveDownLabel }

        // Act
        moveDown.action()
        composeTestRule.waitForIdle()

        // Assert
        val updatedDescription = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        assertEquals(expected, updatedDescription)
    }

    @Test
    fun Should_use_one_based_position_numbers_at_list_boundaries() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }
        val expectedFirst = runBlocking {
            getString(Res.string.reorder_position_announcement, "Alice", 1, 3)
        }
        val expectedLast = runBlocking {
            getString(Res.string.reorder_position_announcement, "Carol", 3, 3)
        }

        // Act
        val firstDescription = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        val lastDescription = composeTestRule.onNodeWithTag("player-row-2").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

        // Assert
        assertEquals(expectedFirst, firstDescription)
        assertEquals(expectedLast, lastDescription)
    }

    @Test
    fun Should_not_expose_drag_handle_description_when_canReorderPlayers_is_false() {
        // Arrange
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = listOf(alice),
                canReorderPlayers = false,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = {},
            )
        }

        // Act & Assert
        composeTestRule.onNodeWithTag("drag-handle-0").assertDoesNotExist()
    }

    @Test
    fun Should_retain_focus_on_player_row_after_move() {
        // Arrange
        var playerList by mutableStateOf(listOf(alice, bob, carol))
        composeTestRule.setContent {
            ReorderablePlayerList(
                players = playerList,
                canReorderPlayers = true,
                onMovePlayer = { from, to ->
                    playerList = playerList.toMutableList().apply { add(to, removeAt(from)) }
                },
                onRemovePlayer = {},
            )
        }
        val moveDownLabel = runBlocking { getString(Res.string.reorder_move_down_action) }
        composeTestRule.onNodeWithTag("player-row-0").requestFocus()
        val node = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
        val actions = node.config.getOrNull(SemanticsActions.CustomActions) ?: emptyList()
        val moveDown = actions.first { it.label == moveDownLabel }

        // Act
        moveDown.action()
        composeTestRule.waitForIdle()

        // Assert
        val newSlotFocused = composeTestRule.onNodeWithTag("player-row-1").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Focused) ?: false
        val oldSlotFocused = composeTestRule.onNodeWithTag("player-row-0").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Focused) ?: false
        assertTrue(newSlotFocused)
        assertFalse(oldSlotFocused)
    }

    // ── INCREMENT 8 (Player Turn Order): Remove button inert during drag ─────

    // NOTE: These two tests assert inertness via the disabled semantics state rather than by
    // physically tapping mid-drag. A real simultaneous tap-while-dragging is a genuine two-finger
    // gesture, and Compose UI test's InputDispatcher tracks gesture state per pointer id across
    // the whole root — injecting a second down() for pointer 0 while the drag handle's gesture is
    // still open throws "Cannot send DOWN event, a gesture is already in progress for pointer 0".
    // Asserting disabled-ness is the precise, direct check for "the button is inert" and sidesteps
    // that InputDispatcher limitation; performClick()/click() are exercised end-to-end once the
    // button is enabled again in the two tests below.

    @Test
    fun Should_not_invoke_onRemovePlayer_when_tapped_on_dragged_row_during_active_drag() {
        // Arrange
        val removedIds = mutableListOf<String>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = { removedIds.add(it) },
            )
        }

        // Act
        // Open the gesture (down + past-slop move) without releasing it yet, so onDragStart
        // fires but the drag stays "in progress".
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + 1f))
        }

        // Assert: the dragged row's own remove button is disabled while the drag is in progress.
        composeTestRule.onNodeWithTag("remove-player-0").assertIsNotEnabled()
        assertTrue(removedIds.isEmpty())

        // Cleanup: release the still-open gesture so it doesn't leak into later tests.
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput { up() }
    }

    @Test
    fun Should_not_invoke_onRemovePlayer_when_tapped_on_different_row_during_active_drag() {
        // Arrange
        val removedIds = mutableListOf<String>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = { removedIds.add(it) },
            )
        }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + 1f))
        }

        // Assert: a DIFFERENT row's (Carol's, row 2) remove button is also disabled list-wide,
        // even though only row 0 is being dragged — rows visually shift during a drag, so an
        // accidental tap on a shifting row is the actual UX risk being prevented.
        composeTestRule.onNodeWithTag("remove-player-2").assertIsNotEnabled()
        assertTrue(removedIds.isEmpty())

        // Cleanup
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput { up() }
    }

    @Test
    fun Should_invoke_onRemovePlayer_after_drag_released_normally() {
        // Arrange
        val removedIds = mutableListOf<String>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = { removedIds.add(it) },
            )
        }

        // Act
        // A small drag that doesn't cross a midpoint (so list order stays Alice, Bob, Carol),
        // completed normally with up().
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + 1f))
            up()
        }
        composeTestRule.onNodeWithTag("remove-player-2").performTouchInput { click() }

        // Assert
        assertEquals(listOf("id-carol"), removedIds)
    }

    @Test
    fun Should_invoke_onRemovePlayer_after_drag_cancelled() {
        // Arrange
        val removedIds = mutableListOf<String>()
        var touchSlopPx = 0f
        composeTestRule.setContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            ReorderablePlayerList(
                players = listOf(alice, bob, carol),
                canReorderPlayers = true,
                onMovePlayer = { _, _ -> },
                onRemovePlayer = { removedIds.add(it) },
            )
        }

        // Act
        composeTestRule.onNodeWithTag("drag-handle-0").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, touchSlopPx + 1f))
            cancel()
        }
        composeTestRule.onNodeWithTag("remove-player-2").performTouchInput { click() }

        // Assert
        assertEquals(listOf("id-carol"), removedIds)
    }
}
