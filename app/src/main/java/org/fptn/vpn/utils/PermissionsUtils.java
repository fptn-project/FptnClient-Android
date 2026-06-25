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

package org.fptn.vpn.utils;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.elvishew.xlog.XLog;

import androidx.core.content.ContextCompat;

import org.fptn.vpn.core.common.Constants;


public class PermissionsUtils {
    private static final String TAG = PermissionsUtils.class.getSimpleName();

    public static boolean isAlwaysOnVpnEnabledByAnotherApp(Context context) {
        try {
            String alwaysOnVpnApp = Settings.Secure.getString(
                    context.getContentResolver(), "always_on_vpn_app");
            return alwaysOnVpnApp != null
                    && !alwaysOnVpnApp.isEmpty()
                    && !alwaysOnVpnApp.equals(context.getPackageName());
        } catch (Exception e) {
            XLog.tag(TAG).w("Failed to check always-on VPN: %s", e.getMessage());
            return false;
        }
    }

    public static boolean isAllOptionalPermissionsGranted(Context context) {
        return checkBackgroundDataTransferRestrictions(context) && checkBatteryOptimizations(context);
    }

    public static boolean checkNotificationEnabled(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.areNotificationsEnabled()) {
            return false;
        }
        // Only check the app's own channels by known IDs — iterating getNotificationChannels()
        // includes system/vendor channels (e.g. on MIUI/ColorOS) that can have IMPORTANCE_NONE
        // even when the user has properly enabled notifications for the app.
        NotificationChannel mainChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        if (mainChannel != null && mainChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            return false;
        }

        boolean isGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        XLog.tag(TAG).i("Notification permission [granted=%b]", isGranted);
        return isGranted;
    }

    public static boolean checkBatteryOptimizations(Context context) {
        if ("xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
            XLog.tag(TAG).i("Battery optimization check skipped [manufacturer=%s, brand=%s, model=%s]",
                    Build.MANUFACTURER, Build.BRAND, Build.MODEL);
            return true;
        }
        boolean isGranted = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            isGranted = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        XLog.tag(TAG).i("Battery optimization exemption [granted=%b, manufacturer=%s, brand=%s, model=%s]",
                isGranted, Build.MANUFACTURER, Build.BRAND, Build.MODEL);
        return isGranted;
    }

    public static boolean checkBackgroundDataTransferRestrictions(Context context) {
        boolean isGranted = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            isGranted = connectivityManager.getRestrictBackgroundStatus() != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
        }
        XLog.tag(TAG).i("Background data transfer [unrestricted=%b]", isGranted);
        return isGranted;
    }
}
