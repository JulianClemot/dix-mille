package com.julian.dixmille.domain.usecase

import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.domain.repository.GameRulesRepository

class FakeGameRulesRepository : GameRulesRepository {
    private var rules: GameRules? = null

    override suspend fun saveRules(rules: GameRules): Result<Unit> {
        this.rules = rules
        return Result.success(Unit)
    }

    override suspend fun getRules(): Result<GameRules> {
        return rules?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("No rules found"))
    }

    override suspend fun deleteRules(): Result<Unit> {
        rules = null
        return Result.success(Unit)
    }
}
