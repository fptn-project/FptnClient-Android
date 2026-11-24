package org.fptn.vpn.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    val displayName: String
        get() = when (this) {
            SYSTEM -> "Авто"
            LIGHT -> "Светлая"
            DARK -> "Тёмная"
        }
}

enum class AppPalette {
    LILAC,
    FOREST,
    OCEAN;

    val displayName: String
        get() = when (this) {
            LILAC -> "Сирень"
            FOREST -> "Лес"
            OCEAN -> "Океан"
        }
}

enum class ContrastLevel {
    STANDARD,
    MEDIUM,
    HIGH;

    val displayName: String
        get() = when (this) {
            STANDARD -> "Обычный"
            MEDIUM -> "Средний"
            HIGH -> "Высокий"
        }
}

enum class NavBarStyle {
    FLOATING,
    DOCKED;

    val displayName: String
        get() = when (this) {
            FLOATING -> "Плавающая"
            DOCKED -> "Закреплённая"
        }
}

@Immutable
data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val amoledMode: Boolean = false,
    val palette: AppPalette = AppPalette.LILAC,
    val contrast: ContrastLevel = ContrastLevel.STANDARD,
    val navBarStyle: NavBarStyle = NavBarStyle.FLOATING,
    val hapticEnabled: Boolean = true,
)

interface ThemeController {
    fun setThemeMode(mode: ThemeMode)
    fun setAmoledMode(enabled: Boolean)
    fun setPalette(palette: AppPalette)
    fun setContrast(contrast: ContrastLevel)
    fun setNavBarStyle(style: NavBarStyle)
    fun setHapticEnabled(enabled: Boolean)
}

val LocalThemeState = compositionLocalOf { ThemeState() }
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    object : ThemeController {
        override fun setThemeMode(mode: ThemeMode) {}
        override fun setAmoledMode(enabled: Boolean) {}
        override fun setPalette(palette: AppPalette) {}
        override fun setContrast(contrast: ContrastLevel) {}
        override fun setNavBarStyle(style: NavBarStyle) {}
        override fun setHapticEnabled(enabled: Boolean) {}
    }
}

/**
 * Singleton state holder managing reactive theme preferences,
 * backed by StateFlow and persistent SharedPreferences.
 */
object ThemeStateHolder : ThemeController {
    private const val PREFS_NAME = "fptn_theme_preferences"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_AMOLED_MODE = "key_amoled_mode"
    private const val KEY_PALETTE = "key_palette"
    private const val KEY_CONTRAST = "key_contrast"
    private const val KEY_NAV_BAR_STYLE = "key_nav_bar_style"
    private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    val current: ThemeState
        get() = _themeState.value

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs

        val loadedMode = prefs.getString(KEY_THEME_MODE, null)?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.DARK

        val loadedAmoled = prefs.getBoolean(KEY_AMOLED_MODE, false)

        val loadedPalette = prefs.getString(KEY_PALETTE, null)?.let { name ->
            runCatching { AppPalette.valueOf(name) }.getOrNull()
        } ?: AppPalette.LILAC

        val loadedContrast = prefs.getString(KEY_CONTRAST, null)?.let { name ->
            runCatching { ContrastLevel.valueOf(name) }.getOrNull()
        } ?: ContrastLevel.STANDARD

        val loadedNavBarStyle = prefs.getString(KEY_NAV_BAR_STYLE, null)?.let { name ->
            runCatching { NavBarStyle.valueOf(name) }.getOrNull()
        } ?: NavBarStyle.FLOATING

        val loadedHaptic = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)

        _themeState.value = ThemeState(
            themeMode = loadedMode,
            amoledMode = loadedAmoled,
            palette = loadedPalette,
            contrast = loadedContrast,
            navBarStyle = loadedNavBarStyle,
            hapticEnabled = loadedHaptic,
        )
    }

    override fun setThemeMode(mode: ThemeMode) {
        _themeState.update { it.copy(themeMode = mode) }
        sharedPreferences?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    override fun setAmoledMode(enabled: Boolean) {
        _themeState.update { it.copy(amoledMode = enabled) }
        sharedPreferences?.edit()?.putBoolean(KEY_AMOLED_MODE, enabled)?.apply()
    }

    override fun setPalette(palette: AppPalette) {
        _themeState.update { it.copy(palette = palette) }
        sharedPreferences?.edit()?.putString(KEY_PALETTE, palette.name)?.apply()
    }

    override fun setContrast(contrast: ContrastLevel) {
        _themeState.update { it.copy(contrast = contrast) }
        sharedPreferences?.edit()?.putString(KEY_CONTRAST, contrast.name)?.apply()
    }

    override fun setNavBarStyle(style: NavBarStyle) {
        _themeState.update { it.copy(navBarStyle = style) }
        sharedPreferences?.edit()?.putString(KEY_NAV_BAR_STYLE, style.name)?.apply()
    }

    override fun setHapticEnabled(enabled: Boolean) {
        _themeState.update { it.copy(hapticEnabled = enabled) }
        sharedPreferences?.edit()?.putBoolean(KEY_HAPTIC_ENABLED, enabled)?.apply()
    }
}
