package com.julian.dixmille.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme with pastel accents.
 * 
 * This is the primary theme for Dix Mille, designed for comfortable
 * extended use with a modern, sophisticated aesthetic.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary - Soft Purple
    primary = Purple40,
    onPrimary = TextPrimary,
    primaryContainer = MediumNavy,
    onPrimaryContainer = Purple80,
    
    // Secondary - Mint Green
    secondary = MintGreen,
    onSecondary = DeepNavy,
    secondaryContainer = DarkMintGreen,
    onSecondaryContainer = MintGreen80,
    
    // Tertiary - Warm Orange
    tertiary = WarmOrange,
    onTertiary = DeepNavy,
    tertiaryContainer = DarkOrange,
    onTertiaryContainer = WarmOrange80,
    
    // Error
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background & Surface
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    // Outline
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

/**
 * Light color scheme (optional fallback).
 * 
 * While the app is designed for dark theme, this provides
 * a light alternative if needed in the future.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Color(0xFF21005D),
    
    secondary = MintGreen,
    onSecondary = Color.White,
    secondaryContainer = MintGreen80,
    onSecondaryContainer = Color(0xFF002114),
    
    tertiary = WarmOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarmOrange80,
    onTertiaryContainer = Color(0xFF2B1700),
    
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

/**
 * Dix Mille theme composable.
 * 
 * By default, always uses dark theme. Set darkTheme parameter to
 * override if needed.
 * 
 * @param darkTheme Whether to use dark theme (default: true, always dark)
 * @param content The composable content to theme
 */
@Composable
fun DixMilleTheme(
    darkTheme: Boolean = true,  // Always dark by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
