package com.julian.dixmille.di

import com.julian.dixmille.core.data.repository.GameRepositoryImpl
import com.julian.dixmille.core.domain.repository.GameRepository
import com.julian.dixmille.core.domain.usecase.GetCurrentGameUseCase
import com.julian.dixmille.feature.game_end.di.gameEndModule
import com.julian.dixmille.feature.game_rules.di.gameRulesModule
import com.julian.dixmille.feature.game_setup.di.gameSetupModule
import com.julian.dixmille.feature.home.di.homeModule
import com.julian.dixmille.feature.score_sheet.di.scoreSheetModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Core data module - provides the game repository.
 */
val coreDataModule = module {
    single { GameRepositoryImpl(get()) } bind GameRepository::class
}

/**
 * Core domain module - provides shared use cases.
 */
val coreDomainModule = module {
    singleOf(::GetCurrentGameUseCase)
}

/**
 * Returns all common modules to be loaded by Koin.
 */
fun commonModules() = listOf(
    coreDataModule,
    coreDomainModule,
    homeModule,
    gameSetupModule,
    scoreSheetModule,
    gameEndModule,
    gameRulesModule
)
