package com.julian.dixmille.di

import com.julian.dixmille.data.repository.GameRepositoryImpl
import com.julian.dixmille.data.repository.GameRulesRepositoryImpl
import com.julian.dixmille.domain.repository.GameRepository
import com.julian.dixmille.domain.repository.GameRulesRepository
import com.julian.dixmille.domain.usecase.*
import com.julian.dixmille.presentation.viewmodel.GameEndViewModel
import com.julian.dixmille.presentation.viewmodel.GameRulesSettingsViewModel
import com.julian.dixmille.presentation.viewmodel.GameSetupViewModel
import com.julian.dixmille.presentation.viewmodel.HomeViewModel
import com.julian.dixmille.presentation.viewmodel.ScoreSheetViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Data module - Repository layer
 */
val dataModule = module {
    single { GameRepositoryImpl(get()) } bind GameRepository::class
    single { GameRulesRepositoryImpl(get()) } bind GameRulesRepository::class
}

/**
 * Domain module - Use cases (all singletons)
 * Use cases are stateless, so they can be shared across the app
 */
val domainModule = module {
    singleOf(::CreateGameUseCase)
    singleOf(::AddScoreEntryUseCase)
    singleOf(::CommitTurnUseCase)
    singleOf(::BustTurnUseCase)
    singleOf(::SkipTurnUseCase)
    singleOf(::UndoLastEntryUseCase)
    singleOf(::UndoLastTurnUseCase)
    singleOf(::GetCurrentGameUseCase)
}

/**
 * Presentation module - ViewModels
 * Each screen has its own ViewModel for separation of concerns
 */
val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::GameSetupViewModel)
    viewModelOf(::ScoreSheetViewModel)
    viewModelOf(::GameEndViewModel)
    viewModelOf(::GameRulesSettingsViewModel)
}

/**
 * Returns all common modules to be loaded by Koin
 */
fun commonModules() = listOf(
    dataModule,
    domainModule,
    presentationModule
)
