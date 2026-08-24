package com.julian.dixmille.feature.game_setup.presentation.component

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plain JVM structural checks (no Compose UI, no Robolectric) verifying that Increment 7's
 * reorder accessibility strings were correctly extracted into string resources, in both locales,
 * and that the production Kotlin source no longer contains hardcoded literals for them.
 */
class ReorderablePlayerListResourcesTest {

    private val projectRoot: File = findProjectRoot(File(".").absoluteFile)

    private val englishStringsFile =
        File(projectRoot, "composeApp/src/commonMain/composeResources/values/strings.xml")
    private val frenchStringsFile =
        File(projectRoot, "composeApp/src/commonMain/composeResources/values-fr/strings.xml")
    private val reorderablePlayerListSource =
        File(
            projectRoot,
            "composeApp/src/commonMain/kotlin/com/julian/dixmille/feature/game_setup/" +
                "presentation/component/ReorderablePlayerList.kt",
        )

    /**
     * Gradle's working directory for this test task isn't guaranteed to be the project root
     * (observed to be `composeApp/` in this project), so walk upward from the current directory
     * until we find the `settings.gradle.kts` marker that identifies the actual project root.
     */
    private fun findProjectRoot(start: File): File {
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        error("Could not locate project root (settings.gradle.kts) from $start")
    }

    @Test
    fun Should_have_reorder_accessibility_strings_in_english_resources() {
        // Arrange
        val content = englishStringsFile.readText()

        // Act & Assert
        assertTrue(content.contains("name=\"reorder_move_up_action\""))
        assertTrue(content.contains("name=\"reorder_move_down_action\""))
        assertTrue(content.contains("name=\"reorder_drag_handle_cd\""))
        assertTrue(content.contains("%1\$s"))
        assertTrue(content.contains("name=\"reorder_position_announcement\""))
        assertTrue(content.contains("%2\$d"))
        assertTrue(content.contains("%3\$d"))
    }

    @Test
    fun Should_have_matching_reorder_accessibility_strings_in_french_resources() {
        // Arrange
        val content = frenchStringsFile.readText()

        // Act & Assert
        assertTrue(content.contains("name=\"reorder_move_up_action\""))
        assertTrue(content.contains("name=\"reorder_move_down_action\""))
        assertTrue(content.contains("name=\"reorder_drag_handle_cd\""))
        assertTrue(content.contains("%1\$s"))
        assertTrue(content.contains("name=\"reorder_position_announcement\""))
        assertTrue(content.contains("%2\$d"))
        assertTrue(content.contains("%3\$d"))
    }

    @Test
    fun Should_not_contain_hardcoded_accessibility_strings_in_reorderable_player_list() {
        // Arrange
        val content = reorderablePlayerListSource.readText()

        // Act & Assert
        assertFalse(content.contains("\"Move up\""))
        assertFalse(content.contains("\"Move down\""))
    }
}
