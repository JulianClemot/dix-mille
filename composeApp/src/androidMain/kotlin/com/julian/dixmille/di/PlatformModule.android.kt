package com.julian.dixmille.di

import com.julian.dixmille.data.source.AndroidLocalStorage
import com.julian.dixmille.data.source.LocalStorage
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Android platform module.
 * 
 * Provides Android-specific dependencies:
 * - LocalStorage using SharedPreferences
 * 
 * The Android Context is automatically provided by Koin via androidContext()
 */
actual val platformModule = module {
    single { AndroidLocalStorage(get()) } bind LocalStorage::class
}
