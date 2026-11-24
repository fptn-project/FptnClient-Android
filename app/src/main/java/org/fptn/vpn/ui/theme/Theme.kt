package org.fptn.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Builds a dynamic Material 3 ColorScheme tailored to the requested palette,
 * light/dark mode, Pure Black AMOLED mode, and contrast level.
 */
fun buildColorScheme(
    isDark: Boolean,
    isAmoled: Boolean,
    palette: AppPalette,
    contrast: ContrastLevel,
): ColorScheme {
    return if (isDark) {
        val baseDark = when (palette) {
            AppPalette.LILAC -> darkColorScheme(
                primary = LilacDarkPrimary,
                onPrimary = LilacDarkOnPrimary,
                primaryContainer = LilacDarkPrimaryContainer,
                onPrimaryContainer = LilacDarkOnPrimaryContainer,
                secondary = LilacDarkSecondary,
                onSecondary = LilacDarkOnSecondary,
                secondaryContainer = LilacDarkSecondaryContainer,
                onSecondaryContainer = LilacDarkOnSecondaryContainer,
                tertiary = LilacDarkTertiary,
                onTertiary = LilacDarkOnTertiary,
                background = LilacDarkBackground,
                onBackground = LilacDarkOnBackground,
                surface = LilacDarkSurface,
                onSurface = LilacDarkOnSurface,
                surfaceVariant = LilacDarkSurfaceVariant,
                onSurfaceVariant = LilacDarkOnSurfaceVariant,
                outline = LilacDarkOutline,
                outlineVariant = LilacDarkOutlineVariant,
            )
            AppPalette.FOREST -> darkColorScheme(
                primary = ForestDarkPrimary,
                onPrimary = ForestDarkOnPrimary,
                primaryContainer = ForestDarkPrimaryContainer,
                onPrimaryContainer = ForestDarkOnPrimaryContainer,
                secondary = ForestDarkSecondary,
                onSecondary = ForestDarkOnSecondary,
                secondaryContainer = ForestDarkSecondaryContainer,
                onSecondaryContainer = ForestDarkOnSecondaryContainer,
                tertiary = ForestDarkTertiary,
                onTertiary = ForestDarkOnTertiary,
                background = ForestDarkBackground,
                onBackground = ForestDarkOnBackground,
                surface = ForestDarkSurface,
                onSurface = ForestDarkOnSurface,
                surfaceVariant = ForestDarkSurfaceVariant,
                onSurfaceVariant = ForestDarkOnSurfaceVariant,
                outline = ForestDarkOutline,
                outlineVariant = ForestDarkOutlineVariant,
            )
            AppPalette.OCEAN -> darkColorScheme(
                primary = OceanDarkPrimary,
                onPrimary = OceanDarkOnPrimary,
                primaryContainer = OceanDarkPrimaryContainer,
                onPrimaryContainer = OceanDarkOnPrimaryContainer,
                secondary = OceanDarkSecondary,
                onSecondary = OceanDarkOnSecondary,
                secondaryContainer = OceanDarkSecondaryContainer,
                onSecondaryContainer = OceanDarkOnSecondaryContainer,
                tertiary = OceanDarkTertiary,
                onTertiary = OceanDarkOnTertiary,
                background = OceanDarkBackground,
                onBackground = OceanDarkOnBackground,
                surface = OceanDarkSurface,
                onSurface = OceanDarkOnSurface,
                surfaceVariant = OceanDarkSurfaceVariant,
                onSurfaceVariant = OceanDarkOnSurfaceVariant,
                outline = OceanDarkOutline,
                outlineVariant = OceanDarkOutlineVariant,
            )
        }

        val amoledAdjusted = if (isAmoled) {
            baseDark.copy(
                background = AmoledBackground,
                surface = AmoledSurface,
                surfaceVariant = AmoledSurfaceVariant,
                outline = AmoledOutline,
                outlineVariant = AmoledOutlineVariant,
            )
        } else {
            baseDark
        }

        when (contrast) {
            ContrastLevel.STANDARD -> amoledAdjusted
            ContrastLevel.MEDIUM -> amoledAdjusted.copy(
                onBackground = Color.White,
                onSurface = Color.White,
                outline = if (isAmoled) Color(0xFF3F3F46) else Color(0xFF6B6673),
            )
            ContrastLevel.HIGH -> amoledAdjusted.copy(
                onBackground = Color.White,
                onSurface = Color.White,
                outline = Color.White,
                outlineVariant = Color(0xFF757575),
            )
        }
    } else {
        // Light mode ignores AMOLED pure black
        val baseLight = when (palette) {
            AppPalette.LILAC -> lightColorScheme(
                primary = LilacLightPrimary,
                onPrimary = LilacLightOnPrimary,
                primaryContainer = LilacLightPrimaryContainer,
                onPrimaryContainer = LilacLightOnPrimaryContainer,
                secondary = LilacLightSecondary,
                onSecondary = LilacLightOnSecondary,
                secondaryContainer = LilacLightSecondaryContainer,
                onSecondaryContainer = LilacLightOnSecondaryContainer,
                tertiary = LilacLightTertiary,
                onTertiary = LilacLightOnTertiary,
                background = LilacLightBackground,
                onBackground = LilacLightOnBackground,
                surface = LilacLightSurface,
                onSurface = LilacLightOnSurface,
                surfaceVariant = LilacLightSurfaceVariant,
                onSurfaceVariant = LilacLightOnSurfaceVariant,
                outline = LilacLightOutline,
                outlineVariant = LilacLightOutlineVariant,
            )
            AppPalette.FOREST -> lightColorScheme(
                primary = ForestLightPrimary,
                onPrimary = ForestLightOnPrimary,
                primaryContainer = ForestLightPrimaryContainer,
                onPrimaryContainer = ForestLightOnPrimaryContainer,
                secondary = ForestLightSecondary,
                onSecondary = ForestLightOnSecondary,
                secondaryContainer = ForestLightSecondaryContainer,
                onSecondaryContainer = ForestLightOnSecondaryContainer,
                tertiary = ForestLightTertiary,
                onTertiary = ForestLightOnTertiary,
                background = ForestLightBackground,
                onBackground = ForestLightOnBackground,
                surface = ForestLightSurface,
                onSurface = ForestLightOnSurface,
                surfaceVariant = ForestLightSurfaceVariant,
                onSurfaceVariant = ForestLightOnSurfaceVariant,
                outline = ForestLightOutline,
                outlineVariant = ForestLightOutlineVariant,
            )
            AppPalette.OCEAN -> lightColorScheme(
                primary = OceanLightPrimary,
                onPrimary = OceanLightOnPrimary,
                primaryContainer = OceanLightPrimaryContainer,
                onPrimaryContainer = OceanLightOnPrimaryContainer,
                secondary = OceanLightSecondary,
                onSecondary = OceanLightOnSecondary,
                secondaryContainer = OceanLightSecondaryContainer,
                onSecondaryContainer = OceanLightOnSecondaryContainer,
                tertiary = OceanLightTertiary,
                onTertiary = OceanLightOnTertiary,
                background = OceanLightBackground,
                onBackground = OceanLightOnBackground,
                surface = OceanLightSurface,
                onSurface = OceanLightOnSurface,
                surfaceVariant = OceanLightSurfaceVariant,
                onSurfaceVariant = OceanLightOnSurfaceVariant,
                outline = OceanLightOutline,
                outlineVariant = OceanLightOutlineVariant,
            )
        }

        when (contrast) {
            ContrastLevel.STANDARD -> baseLight
            ContrastLevel.MEDIUM -> baseLight.copy(
                onBackground = Color.Black,
                onSurface = Color.Black,
                outline = Color(0xFF49454F),
            )
            ContrastLevel.HIGH -> baseLight.copy(
                onBackground = Color.Black,
                onSurface = Color.Black,
                outline = Color.Black,
                outlineVariant = Color(0xFF2C2C2E),
            )
        }
    }
}

/**
 * App-wide Material 3 theme for FptnClient Compose UI.
 *
 * Automatically subscribes to [ThemeStateHolder] reactively and provides
 * [LocalThemeState] and [LocalThemeController] down the composition hierarchy.
 *
 * Supports:
 * - Direct usage: `FptnTheme { ... }` (uses reactive ThemeStateHolder)
 * - Custom state: `FptnTheme(themeState = customState) { ... }`
 * - Legacy callers: `FptnTheme(darkTheme = true) { ... }`
 */
@Composable
fun FptnTheme(
    themeState: ThemeState? = null,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ThemeStateHolder.init(context)
    }

    val stateFromHolder by ThemeStateHolder.themeState.collectAsState()
    val baseState = themeState ?: stateFromHolder
    val activeState = if (darkTheme != null) {
        baseState.copy(themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT)
    } else {
        baseState
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (activeState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = buildColorScheme(
        isDark = isDark,
        isAmoled = activeState.amoledMode,
        palette = activeState.palette,
        contrast = activeState.contrast,
    )

    CompositionLocalProvider(
        LocalThemeState provides activeState,
        LocalThemeController provides ThemeStateHolder,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FptnTypography,
            content = content,
        )
    }
}

/**
 * Extension property to retrieve the appropriate card container color
 * based on whether Pure Black AMOLED mode is currently active.
 */
val MaterialTheme.cardContainerColor: Color
    @Composable
    get() {
        val themeState = LocalThemeState.current
        val systemDark = isSystemInDarkTheme()
        val isDark = when (themeState.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        return if (isDark && themeState.amoledMode) {
            when (themeState.palette) {
                AppPalette.LILAC -> Color(0xFF17141C)
                AppPalette.FOREST -> Color(0xFF111712)
                AppPalette.OCEAN -> Color(0xFF11151D)
            }
        } else {
            colorScheme.surfaceVariant
        }
    }
