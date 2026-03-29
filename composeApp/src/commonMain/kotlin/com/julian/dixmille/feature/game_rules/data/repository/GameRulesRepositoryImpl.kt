package com.julian.dixmille.feature.game_rules.data.repository

import com.julian.dixmille.core.data.mapper.toDomain
import com.julian.dixmille.core.data.mapper.toDto
import com.julian.dixmille.core.data.model.GameRulesDto
import com.julian.dixmille.core.data.source.LocalStorage
import com.julian.dixmille.core.domain.model.GameRules
import com.julian.dixmille.core.domain.repository.GameRulesRepository
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
        val dto = rules.toDto()
        val rulesJson = json.encodeToString(GameRulesDto.serializer(), dto)
        localStorage.saveString(RULES_KEY, rulesJson)
    }

    override suspend fun getRules(): Result<GameRules> = runCatching {
        val rulesJson = localStorage.getString(RULES_KEY)
            ?: throw NoSuchElementException("No saved rules found")
        json.decodeFromString(GameRulesDto.serializer(), rulesJson).toDomain()
    }

    override suspend fun deleteRules(): Result<Unit> = runCatching {
        localStorage.remove(RULES_KEY)
    }

    companion object {
        private const val RULES_KEY = "game_rules"
    }
}
