package com.julian.dixmille.feature.game_setup.di

import com.julian.dixmille.feature.game_setup.domain.usecase.CreateGameUseCase
import com.julian.dixmille.feature.game_setup.presentation.viewmodel.GameSetupViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val gameSetupModule = module {
    singleOf(::CreateGameUseCase)
    viewModelOf(::GameSetupViewModel)
}
