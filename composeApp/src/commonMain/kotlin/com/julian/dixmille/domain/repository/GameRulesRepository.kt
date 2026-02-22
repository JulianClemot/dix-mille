package com.julian.dixmille.domain.repository

import com.julian.dixmille.domain.model.GameRules

interface GameRulesRepository {
    suspend fun saveRules(rules: GameRules): Result<Unit>
    suspend fun getRules(): Result<GameRules>
    suspend fun deleteRules(): Result<Unit>
}
