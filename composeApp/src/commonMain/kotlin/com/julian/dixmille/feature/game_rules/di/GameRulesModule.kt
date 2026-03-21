package com.julian.dixmille.feature.game_rules.di

import com.julian.dixmille.core.domain.repository.GameRulesRepository
import com.julian.dixmille.feature.game_rules.data.repository.GameRulesRepositoryImpl
import com.julian.dixmille.feature.game_rules.presentation.viewmodel.GameRulesSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val gameRulesModule = module {
    single { GameRulesRepositoryImpl(get()) } bind GameRulesRepository::class
    viewModelOf(::GameRulesSettingsViewModel)
}
