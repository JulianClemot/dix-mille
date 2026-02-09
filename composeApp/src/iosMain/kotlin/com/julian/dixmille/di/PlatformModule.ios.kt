package com.julian.dixmille.di

import com.julian.dixmille.data.source.IOSLocalStorage
import com.julian.dixmille.data.source.LocalStorage
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * iOS platform module.
 * 
 * Provides iOS-specific dependencies:
 * - LocalStorage using NSUserDefaults
 */
actual val platformModule = module {
    single { IOSLocalStorage() } bind LocalStorage::class
}
