package com.roxxer.zenith.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// EL INTERRUPTOR MÁGICO QUE RECORRE TODA LA APP
val LocalThemeIsDark = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    background = RealDarkBackground, surface = RealDarkSurface,
    onBackground = RealDarkTextPrimary, onSurface = RealDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground, surface = LightSurface,
    onBackground = LightTextPrimary, onSurface = LightTextPrimary
)

@Composable
fun ZenithTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // CORRECCIÓN AQUÍ: Build.VERSION_CODES.S en lugar de solo CODES.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // AÑADIMOS EL PROVEEDOR DEL INTERRUPTOR
    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}