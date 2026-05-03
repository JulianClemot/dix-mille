package com.julian.dixmille.di

import androidx.room3.Room
import com.julian.dixmille.core.data.db.AppDatabase
import com.julian.dixmille.core.data.source.AndroidLocalStorage
import com.julian.dixmille.core.data.source.LocalStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule = module {
    single { AndroidLocalStorage(get()) } bind LocalStorage::class
    single {
        Room.databaseBuilder<AppDatabase>(androidContext(), "dixmille.db")
            .build()
    }
    single { get<AppDatabase>().playerDao() }
}
