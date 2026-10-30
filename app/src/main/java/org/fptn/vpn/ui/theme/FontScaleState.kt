package org.fptn.vpn.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.fptn.vpn.core.common.Constants
import org.fptn.vpn.utils.SharedPrefUtils

/**
 * Global application font size state.
 *
 * Uses [mutableStateOf], so any access to these fields from an @Composable function
 * automatically subscribes to changes. When the user changes the value in Settings,
 * the entire UI is immediately recomposed without restarting the app.
 *
 * Loaded once in [org.fptn.vpn.App.onCreate] when the app starts.
 * After importing settings from a backup, [load] must be called manually.
 */
object FontScaleState {
    var useSystem: Boolean by mutableStateOf(true)
        private set

    var index: Int by mutableStateOf(Constants.FONT_SCALE_DEFAULT_INDEX)
        private set

    /**
     * Loads values from SharedPreferences. Safe to call multiple times.
     */
    fun load(context: Context) {
        useSystem = SharedPrefUtils.getUseSystemFontSize(context)
        index = SharedPrefUtils.getFontScaleIndex(context)
    }

    /**
     * Updates "Use system font size" in both the state and SharedPreferences.
     * All Composable functions that read [useSystem] will automatically recompose.
     */
    fun setUseSystem(context: Context, value: Boolean) {
        useSystem = value
        SharedPrefUtils.saveUseSystemFontSize(context, value)
    }

    /**
     * Updates the custom font size index in both the state and SharedPreferences.
     * Active only when [useSystem] == false.
     */
    fun setIndex(context: Context, value: Int) {
        index = value.coerceIn(0, Constants.FONT_SCALE_VALUES.lastIndex)
        SharedPrefUtils.saveFontScaleIndex(context, index)
    }

    /**
     * Returns the effective fontScale to apply to LocalDensity.
     * If [useSystem] == true, returns null to indicate that the system fontScale should be used.
     */
    fun effectiveFontScaleOrNull(): Float? {
        return if (useSystem) null
        else Constants.FONT_SCALE_VALUES.getOrElse(index) { 1.0f }
    }
}