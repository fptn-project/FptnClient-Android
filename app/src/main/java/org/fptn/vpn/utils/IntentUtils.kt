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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.elvishew.xlog.XLog
import org.fptn.vpn.R

private const val TAG = "IntentUtils"

/**
 * Starts an activity for [intent] only if a handler exists on this device.
 *
 * Android TV misses several phone system screens (the VPN settings panel, share
 * targets, a browser) — an unguarded launch would crash with ActivityNotFoundException.
 * Degrades to a toast instead.
 */
fun Context.startIfResolvable(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        XLog.tag(TAG).w("No activity to handle [action=%s] (Android TV?) — %s", intent.action, e.message)
        Toast.makeText(this, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
    }
}
