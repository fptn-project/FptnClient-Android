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

package org.fptn.vpn.ui.settings

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fptn.vpn.core.common.Constants
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.vpn.FptnService

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)

    fun deleteAllServers() {
        viewModelScope.launch(Dispatchers.IO) {
            // Stop background services FIRST — otherwise their foreground notifications outlive
            // the session and their PendingIntents can still re-open MainActivity on an
            // authenticated route even after the user has signed out. Cancelling the notification
            // IDs explicitly is also defensive: stopForeground(STOP_FOREGROUND_REMOVE) only runs
            // when the service processes its stop intent, which can race the table wipe.
            try {
                SniCheckerService.stopChecking(getApplication())
            } catch (e: Exception) {
                // SniCheckerService may not be running — ignore.
            }
            try {
                FptnService.startToDisconnect(getApplication())
            } catch (e: Exception) {
                // FptnService may not be running — ignore.
            }
            try {
                val nm = NotificationManagerCompat.from(getApplication())
                nm.cancel(Constants.SNI_CHECKER_NOTIFICATION_ID)
                nm.cancel(Constants.MAIN_CONNECTED_NOTIFICATION_ID)
                nm.cancel(Constants.ERROR_CONNECTED_NOTIFICATION_ID)
                nm.cancel(Constants.INFO_NOTIFICATION_NOTIFICATION_ID)
            } catch (e: Exception) {
                // Best-effort cleanup.
            }

            appDatabase.serverDAO().deleteAll()
            appDatabase.tokenDAO().deleteAll()
        }
    }
}
