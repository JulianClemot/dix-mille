package com.julian.dixmille.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for entering a custom score.
 * 
 * Note: This is kept for backward compatibility but is no longer used.
 * The inline CustomScoreInput component is now preferred.
 */
@Composable
fun CustomScoreDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var scoreText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the score points:")
                
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = {
                        scoreText = it
                        error = null
                    },
                    label = { Text("Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreText.toIntOrNull()
                    when {
                        score == null -> error = "Please enter a valid number"
                        score <= 0 -> error = "Score must be positive"
                        else -> onConfirm(score)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Game end dialog - DEPRECATED.
 * 
 * Replaced by GameEndScreen with full-screen winner celebration.
 * This is kept for backward compatibility but should not be used.
 */
