package com.julian.dixmille.di

import org.koin.core.module.Module

/**
 * Platform-specific module declaration.
 * 
 * This module provides platform-specific dependencies like LocalStorage.
 * Each platform (Android, iOS) must provide an actual implementation.
 */
expect val platformModule: Module
