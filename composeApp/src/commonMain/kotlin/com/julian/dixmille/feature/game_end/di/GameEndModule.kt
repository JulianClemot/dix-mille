package com.julian.dixmille.feature.game_end.di

import com.julian.dixmille.feature.game_end.presentation.viewmodel.GameEndViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val gameEndModule = module {
    viewModelOf(::GameEndViewModel)
}
