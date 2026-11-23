/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

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
 * */
fun Context.isTv(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)