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

package org.fptn.vpn.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.utils.PermissionsUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        if (!SharedPrefUtils.getAutoStartOnBoot(context)) {
            XLog.tag(TAG).i("Auto-start on boot is disabled — skipping");
            return;
        }

        XLog.tag(TAG).i("Boot completed — auto-start enabled, checking session...");

        // Use goAsync() to do DB check on a background thread without blocking.
        // BroadcastReceiver.onReceive runs on the main thread — a direct Room
        // query would crash (AppDatabase does not allow main-thread queries).
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                // Check if user has a valid session (servers + tokens in DB).
                AppDatabase db = AppDatabase.getInstance(context);
                if (db.serverDAO().getCountSync() == 0 || db.tokenDAO().getCount() == 0) {
                    XLog.tag(TAG).i("No servers/tokens in DB — not logged in, skipping auto-start");
                    return;
                }

                // Check if VPN permission is already granted.
                // VpnService.prepare() returns null when permission is already granted;
                // a non-null Intent means a consent dialog is needed — we can't show
                // a dialog from a BroadcastReceiver, so we skip auto-start.
                if (VpnService.prepare(context) != null) {
                    XLog.tag(TAG).w("VPN permission not granted — cannot auto-start on boot");
                    return;
                }

                // Check notification permission (Android 13+).
                // Without it, startForeground() throws ForegroundServiceStartNotAllowedException.
                if (!PermissionsUtils.checkNotificationEnabled(context)) {
                    XLog.tag(TAG).w("Notification permission not granted — cannot auto-start on boot");
                    return;
                }

                // Start VPN connection.
                XLog.tag(TAG).i("Session valid, VPN permission granted — starting VPN");
                FptnService.startToConnect(context);
            } catch (Exception e) {
                XLog.tag(TAG).e("Failed to auto-start VPN on boot: %s", e.getMessage());
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}