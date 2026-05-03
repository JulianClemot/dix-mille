package com.julian.dixmille.feature.game_setup.di

import com.julian.dixmille.core.data.repository.SavedPlayerRepositoryImpl
import com.julian.dixmille.core.domain.repository.SavedPlayerRepository
import com.julian.dixmille.feature.game_setup.domain.usecase.AddSavedPlayerUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.GetSavedPlayersUseCase
import com.julian.dixmille.feature.game_setup.domain.usecase.UpdateLastPlayedAtUseCase
import org.koin.dsl.bind
import org.koin.dsl.module

val playerLibraryModule = module {
    single { SavedPlayerRepositoryImpl(get()) } bind SavedPlayerRepository::class
    factory { GetSavedPlayersUseCase(get()) }
    factory { AddSavedPlayerUseCase(get()) }
    factory { UpdateLastPlayedAtUseCase(get()) }
}
