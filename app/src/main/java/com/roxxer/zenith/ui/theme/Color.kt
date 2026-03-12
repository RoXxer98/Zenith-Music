package com.roxxer.zenith.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- MODO CLARO ---
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF6B7280)

// --- MODO OSCURO (Tu diseño original) ---
val RealDarkBackground = Color(0xFF0B0F14) // Negro profundo
val RealDarkSurface = Color(0xFF121821)    // Superficie
val RealDarkTextPrimary = Color(0xFFFFFFFF) // Texto blanco
val RealDarkTextSecondary = Color(0xFF8B93A6) // Texto gris

// ====================================================================
// VARIABLES DINÁMICAS MÁGICAS (Leen el interruptor global de Theme.kt)
// ====================================================================
val DarkBackground: Color
    @Composable get() = if (LocalThemeIsDark.current) RealDarkBackground else LightBackground

val DarkSurface: Color
    @Composable get() = if (LocalThemeIsDark.current) RealDarkSurface else LightSurface

val DarkTextPrimary: Color
    @Composable get() = if (LocalThemeIsDark.current) RealDarkTextPrimary else LightTextPrimary

val DarkTextSecondary: Color
    @Composable get() = if (LocalThemeIsDark.current) RealDarkTextSecondary else LightTextSecondary

// El color por defecto en caso de emergencia
val DarkPrimary = Color(0xFF1ED760)
val DarkAccent = Color(0xFF3BF0A4)