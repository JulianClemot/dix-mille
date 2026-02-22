package com.julian.dixmille.data.repository

import com.julian.dixmille.data.source.LocalStorage
import com.julian.dixmille.domain.model.GameRules
import com.julian.dixmille.domain.repository.GameRulesRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameRulesRepositoryImpl(
    private val localStorage: LocalStorage
) : GameRulesRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveRules(rules: GameRules): Result<Unit> = runCatching {
        val rulesJson = json.encodeToString(rules)
        localStorage.saveString(RULES_KEY, rulesJson)
    }

    override suspend fun getRules(): Result<GameRules> = runCatching {
        val rulesJson = localStorage.getString(RULES_KEY)
            ?: throw NoSuchElementException("No saved rules found")
        json.decodeFromString<GameRules>(rulesJson)
    }

    override suspend fun deleteRules(): Result<Unit> = runCatching {
        localStorage.remove(RULES_KEY)
    }

    companion object {
        private const val RULES_KEY = "game_rules"
    }
}
