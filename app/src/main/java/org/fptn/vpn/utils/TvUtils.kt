package org.fptn.vpn.utils

import android.content.Context
import android.content.pm.PackageManager

/**
 * True on Android TV hardware only.
 *
 * Hardware-based (PackageManager.FEATURE_LEANBACK), so it never flips true on
 * phones in cast/desktop mode or under MIUI uiMode quirks — unlike
 * Configuration.uiMode, which is a runtime config and can report
 * UI_MODE_TYPE_TELEVISION on a phone mirroring to an external display.
 *
 * Matches the detection used by SettingsBackupManager for TV-specific
 * settings overrides (force landscape, system font size).
 */
fun Context.isTv(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)