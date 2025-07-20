package org.fptn.vpn.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = Color.md_theme_light_primary,
        onPrimary = Color.md_theme_light_onPrimary,
        primaryContainer = Color.md_theme_light_primaryContainer,
        onPrimaryContainer = Color.md_theme_light_onPrimaryContainer,
        secondary = Color.md_theme_light_secondary,
        onSecondary = Color.md_theme_light_onSecondary,
        secondaryContainer = Color.md_theme_light_secondaryContainer,
        onSecondaryContainer = Color.md_theme_light_onSecondaryContainer,
        tertiary = Color.md_theme_light_tertiary,
        onTertiary = Color.md_theme_light_onTertiary,
        tertiaryContainer = Color.md_theme_light_tertiaryContainer,
        onTertiaryContainer = Color.md_theme_light_onTertiaryContainer,
        background = Color.md_theme_light_background,
        onBackground = Color.md_theme_light_onBackground,
        surface = Color.md_theme_light_surface,
        onSurface = Color.md_theme_light_onSurface,
        surfaceVariant = Color.md_theme_light_surfaceVariant,
        onSurfaceVariant = Color.md_theme_light_onSurfaceVariant,
        surfaceTint = Color.md_theme_light_surfaceTint,
        inverseSurface = Color.md_theme_light_inverseSurface,
        inverseOnSurface = Color.md_theme_light_inverseOnSurface,
        error = Color.md_theme_light_error,
        onError = Color.md_theme_light_onError,
        errorContainer = Color.md_theme_light_errorContainer,
        onErrorContainer = Color.md_theme_light_onErrorContainer,
        outline = Color.md_theme_light_outline,
    )
private val DarkColorScheme =
    darkColorScheme(
        primary = Color.md_theme_dark_primary,
        onPrimary = Color.md_theme_dark_onPrimary,
        primaryContainer = Color.md_theme_dark_primaryContainer,
        onPrimaryContainer = Color.md_theme_dark_onPrimaryContainer,
        secondary = Color.md_theme_dark_secondary,
        onSecondary = Color.md_theme_dark_onSecondary,
        secondaryContainer = Color.md_theme_dark_secondaryContainer,
        onSecondaryContainer = Color.md_theme_dark_onSecondaryContainer,
        tertiary = Color.md_theme_dark_tertiary,
        onTertiary = Color.md_theme_dark_onTertiary,
        tertiaryContainer = Color.md_theme_dark_tertiaryContainer,
        onTertiaryContainer = Color.md_theme_dark_onTertiaryContainer,
        background = Color.md_theme_dark_background,
        onBackground = Color.md_theme_dark_onBackground,
        surface = Color.md_theme_dark_surface,
        onSurface = Color.md_theme_dark_onSurface,
        surfaceVariant = Color.md_theme_dark_surfaceVariant,
        onSurfaceVariant = Color.md_theme_dark_onSurfaceVariant,
        surfaceTint = Color.md_theme_dark_surfaceTint,
        inverseSurface = Color.md_theme_dark_inverseSurface,
        inverseOnSurface = Color.md_theme_dark_inverseOnSurface,
        error = Color.md_theme_dark_error,
        onError = Color.md_theme_dark_onError,
        errorContainer = Color.md_theme_dark_errorContainer,
        onErrorContainer = Color.md_theme_dark_onErrorContainer,
        outline = Color.md_theme_dark_outline,
    )

@Composable
fun PvnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> {
                DarkColorScheme
            }
            else -> {
                LightColorScheme
            }
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MmTypography,
        content = content,
    )
}
