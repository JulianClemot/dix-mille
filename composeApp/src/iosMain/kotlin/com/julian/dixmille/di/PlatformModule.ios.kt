package com.julian.dixmille.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.julian.dixmille.core.data.db.AppDatabase
import com.julian.dixmille.core.data.source.IOSLocalStorage
import com.julian.dixmille.core.data.source.LocalStorage
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val platformModule = module {
    single { IOSLocalStorage() } bind LocalStorage::class
    single {
        val dbPath = NSHomeDirectory() + "/dixmille.db"
        Room.databaseBuilder<AppDatabase>(dbPath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().playerDao() }
}
