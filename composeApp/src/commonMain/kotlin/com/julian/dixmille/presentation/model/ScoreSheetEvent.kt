package com.julian.dixmille.presentation.model

/**
 * Events for the Score Sheet screen (active game play).
 */
sealed class ScoreSheetEvent {
    
    /**
     * Add a score entry to the current turn.
     */
    data class AddScore(
        val points: Int,
        val isPreset: Boolean = false,
        val label: String? = null
    ) : ScoreSheetEvent()
    
    /**
     * Remove the last score entry from the current turn.
     */
    data object UndoLastEntry : ScoreSheetEvent()
    
    /**
     * Undo the last committed turn (revert to previous player state).
     */
    data object UndoLastTurn : ScoreSheetEvent()
    
    /**
     * Mark the current turn as busted (counts toward 3-bust penalty).
     */
    data object BustTurn : ScoreSheetEvent()
    
    /**
     * Skip the current turn voluntarily (does NOT count as bust).
     */
    data object SkipTurn : ScoreSheetEvent()
    
    /**
     * Dismiss any error message.
     */
    data object DismissError : ScoreSheetEvent()
}
