package com.julian.dixmille.presentation.model

import com.julian.dixmille.domain.model.Game
import com.julian.dixmille.domain.model.GamePhase
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.domain.model.ScoreEntry

/**
 * UI state for the Score Sheet screen.
 */
data class ScoreSheetUiState(
    val game: Game? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * The current player whose turn it is.
     */
    val currentPlayer: Player?
        get() = game?.currentPlayer
    
    /**
     * Current turn entries.
     */
    val currentTurnEntries: List<ScoreEntry>
        get() = currentPlayer?.currentTurn?.entries ?: emptyList()
    
    /**
     * Current turn total points.
     */
    val currentTurnTotal: Int
        get() = currentPlayer?.currentTurn?.turnTotal ?: 0
    
    /**
     * Whether the current player has entered the game.
     */
    val currentPlayerHasEntered: Boolean
        get() = currentPlayer?.hasEnteredGame ?: false
    
    /**
     * Whether the current turn can be committed.
     * Must have points and meet entry requirement if not entered.
     */
    val canCommitTurn: Boolean
        get() {
            if (currentTurnTotal == 0) return false
            if (!currentPlayerHasEntered && currentTurnTotal < 500) return false
            return true
        }
    
    /**
     * Whether undo is available (has entries to undo).
     */
    val canUndo: Boolean
        get() = currentTurnEntries.isNotEmpty()
    
    /**
     * Whether undo turn is available (has turn history to undo).
     */
    val canUndoTurn: Boolean
        get() = (game?.turnHistory?.size ?: 0) > 0
    
    /**
     * Whether the game is in final round.
     */
    val isFinalRound: Boolean
        get() = game?.gamePhase == GamePhase.FINAL_ROUND
    
    /**
     * Whether the game has ended.
     */
    val isGameEnded: Boolean
        get() = game?.gamePhase == GamePhase.ENDED
    
    /**
     * The winner of the game (if ended).
     */
    val winner: Player?
        get() = if (isGameEnded) {
            game?.players?.maxByOrNull { it.totalScore }
        } else null
    
    /**
     * All players sorted by score (descending).
     */
    val playersByScore: List<Player>
        get() = game?.players?.sortedByDescending { it.totalScore } ?: emptyList()
}
