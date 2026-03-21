package com.julian.dixmille.core.domain.repository

import com.julian.dixmille.core.domain.model.GameRules

interface GameRulesRepository {
    suspend fun saveRules(rules: GameRules): Result<Unit>
    suspend fun getRules(): Result<GameRules>
    suspend fun deleteRules(): Result<Unit>
}
