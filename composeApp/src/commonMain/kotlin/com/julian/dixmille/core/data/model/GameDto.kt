package com.julian.dixmille.core.data.model

import com.julian.dixmille.core.domain.model.GamePhase
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Game.
 *
 * Uses only primitive types so that the domain model is decoupled
 * from kotlinx.serialization. JSON field names match the historic
 * on-disk format exactly — do NOT rename fields.
 */
@Serializable
data class GameDto(
    val id: String,
    val players: List<PlayerDto>,
    val targetScore: Int,
    val currentPlayerIndex: Int,
    val gamePhase: GamePhase,
    val triggeringPlayerId: String?,
    val createdAt: Long,
    val turnHistory: List<TurnRecordDto>,
    val roundNumber: Int,
    val rules: GameRulesDto
)

@Serializable
data class PlayerDto(
    val id: String,
    val name: String,
    val totalScore: Int,
    val hasEnteredGame: Boolean,
    val currentTurn: TurnDto?,
    val hasPlayedFinalRound: Boolean,
    val consecutiveBusts: Int
)

@Serializable
data class TurnDto(
    val id: String,
    val entries: List<ScoreEntryDto>,
    val isBusted: Boolean
)

@Serializable
data class ScoreEntryDto(
    val id: String,
    val points: Int,
    val type: com.julian.dixmille.core.domain.model.ScoreType,
    val label: String?
)

@Serializable
data class TurnRecordDto(
    val roundNumber: Int,
    val playerId: String,
    val points: Int,
    val outcome: com.julian.dixmille.core.domain.model.TurnOutcome,
    val previousScore: Int
)

@Serializable
data class GameRulesDto(
    val targetScore: Int,
    val entryMinimumScore: Int,
    val consecutiveBustsForPenalty: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val enableBustPenalty: Boolean,
    val enableFinalRound: Boolean
)
