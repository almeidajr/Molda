package com.almeidajr.molda.ui.theme

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Composable
fun MoldaTheme(isDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    SystemBarIcons(isDarkTheme)
    MaterialTheme(
        colorScheme = moldaColorScheme(LocalContext.current, isDarkTheme),
        content = content,
    )
}

/** Keeps the system bar icons readable on the theme's light or dark background. */
@Composable
private fun SystemBarIcons(isDarkTheme: Boolean) {
    val window = LocalActivity.current?.window ?: return
    // WindowCompat.enableEdgeToEdge leaves the system bar icon colours to the app.
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}

/** The scheme [MoldaTheme] applies; device tests use it to know the guide's bound colours. */
internal fun moldaColorScheme(context: Context, isDarkTheme: Boolean): ColorScheme =
    if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
