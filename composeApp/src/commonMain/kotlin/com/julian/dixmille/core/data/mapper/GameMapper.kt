package com.julian.dixmille.core.data.mapper

import com.julian.dixmille.core.data.model.GameDto
import com.julian.dixmille.core.data.model.GameRulesDto
import com.julian.dixmille.core.data.model.PlayerDto
import com.julian.dixmille.core.data.model.ScoreEntryDto
import com.julian.dixmille.core.data.model.TurnDto
import com.julian.dixmille.core.data.model.TurnRecordDto
import com.julian.dixmille.core.domain.model.Game
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.model.Player
import com.julian.dixmille.core.domain.model.ScoreEntry
import com.julian.dixmille.core.domain.model.Turn
import com.julian.dixmille.core.domain.model.TurnRecord
import com.julian.dixmille.core.domain.model.vo.BustCount
import com.julian.dixmille.core.domain.model.vo.EntryId
import com.julian.dixmille.core.domain.model.vo.EntryMinimumScore
import com.julian.dixmille.core.domain.model.vo.GameId
import com.julian.dixmille.core.domain.model.vo.PlayerId
import com.julian.dixmille.core.domain.model.vo.PlayerName
import com.julian.dixmille.core.domain.model.vo.Score
import com.julian.dixmille.core.domain.model.vo.TargetScore
import com.julian.dixmille.core.domain.model.vo.TurnId

// --- Domain → DTO ---

fun Game.toDto(): GameDto = GameDto(
    id = id.value,
    players = players.map { it.toDto() },
    targetScore = targetScore.value,
    currentPlayerIndex = currentPlayerIndex,
    gamePhase = gamePhase,
    triggeringPlayerId = triggeringPlayerId?.value,
    createdAt = createdAt,
    turnHistory = turnHistory.map { it.toDto() },
    roundNumber = roundNumber,
    rules = rules.toDto()
)

fun Player.toDto(): PlayerDto = PlayerDto(
    id = id.value,
    name = name.value,
    totalScore = totalScore.value,
    hasEnteredGame = hasEnteredGame,
    currentTurn = currentTurn?.toDto(),
    hasPlayedFinalRound = hasPlayedFinalRound,
    consecutiveBusts = consecutiveBusts.value
)

fun Turn.toDto(): TurnDto = TurnDto(
    id = id.value,
    entries = entries.map { it.toDto() },
    isBusted = isBusted
)

fun ScoreEntry.toDto(): ScoreEntryDto = ScoreEntryDto(
    id = id.value,
    points = points.value,
    type = type,
    label = label
)

fun TurnRecord.toDto(): TurnRecordDto = TurnRecordDto(
    roundNumber = roundNumber,
    playerId = playerId.value,
    points = points.value,
    outcome = outcome,
    previousScore = previousScore.value
)

fun GameRules.toDto(): GameRulesDto = GameRulesDto(
    targetScore = targetScore.value,
    entryMinimumScore = entryMinimumScore.value,
    consecutiveBustsForPenalty = consecutiveBustsForPenalty,
    minPlayers = minPlayers,
    maxPlayers = maxPlayers,
    enableBustPenalty = enableBustPenalty,
    enableFinalRound = enableFinalRound
)

// --- DTO → Domain ---

fun GameDto.toDomain(): Game = Game(
    id = GameId(id),
    players = players.map { it.toDomain() },
    targetScore = TargetScore(targetScore),
    currentPlayerIndex = currentPlayerIndex,
    gamePhase = gamePhase,
    triggeringPlayerId = triggeringPlayerId?.let { PlayerId(it) },
    createdAt = createdAt,
    turnHistory = turnHistory.map { it.toDomain() },
    roundNumber = roundNumber,
    rules = rules.toDomain()
)

fun PlayerDto.toDomain(): Player = Player(
    id = PlayerId(id),
    name = PlayerName(name),
    totalScore = Score(totalScore),
    hasEnteredGame = hasEnteredGame,
    currentTurn = currentTurn?.toDomain(),
    hasPlayedFinalRound = hasPlayedFinalRound,
    consecutiveBusts = BustCount(consecutiveBusts)
)

fun TurnDto.toDomain(): Turn = Turn(
    id = TurnId(id),
    entries = entries.map { it.toDomain() },
    isBusted = isBusted
)

fun ScoreEntryDto.toDomain(): ScoreEntry = ScoreEntry(
    id = EntryId(id),
    points = Score(points),
    type = type,
    label = label
)

fun TurnRecordDto.toDomain(): TurnRecord = TurnRecord(
    roundNumber = roundNumber,
    playerId = PlayerId(playerId),
    points = Score(points),
    outcome = outcome,
    previousScore = Score(previousScore)
)

fun GameRulesDto.toDomain(): GameRules = GameRules(
    targetScore = TargetScore(targetScore),
    entryMinimumScore = if (entryMinimumScore == 0) EntryMinimumScore.ZERO else EntryMinimumScore(entryMinimumScore),
    consecutiveBustsForPenalty = consecutiveBustsForPenalty,
    minPlayers = minPlayers,
    maxPlayers = maxPlayers,
    enableBustPenalty = enableBustPenalty,
    enableFinalRound = enableFinalRound
)
