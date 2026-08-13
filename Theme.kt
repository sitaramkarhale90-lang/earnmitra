package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = NavyDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = SurfaceMint,
    secondary = GoldAccent,
    onSecondary = TextPrimary,
    background = NavyDark,
    surface = NavyLight,
    onBackground = SurfaceMint,
    onSurface = SurfaceMint
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceCard,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = GoldAccent,
    onSecondary = TextPrimary,
    secondaryContainer = GoldLight,
    onSecondaryContainer = TextPrimary,
    background = SurfaceMint,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun EarnMitraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep EarnMitra brand colors consistent across Android versions
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
