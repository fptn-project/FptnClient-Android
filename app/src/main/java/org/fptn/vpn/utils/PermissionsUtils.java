package org.fptn.vpn.utils;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class PermissionsUtils {
    private static final String TAG = PermissionsUtils.class.getSimpleName();

    public static boolean isAllPermissionsGranted(Context context) {
        return checkBackgroundDataTransferRestrictions(context) && checkBatteryOptimizations(context) && checkNotificationPermission(context);
    }

    public static boolean checkNotificationPermission(Context context) {
        boolean isGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        Log.i(TAG, "checkNotificationPermission: " + isGranted);
        return isGranted;
    }

    public static boolean checkBatteryOptimizations(Context context) {
        boolean isGranted = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            isGranted = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        Log.i(TAG, "checkBatteryOptimizationsPermission: " + isGranted);
        return isGranted;
    }

    public static boolean checkBackgroundDataTransferRestrictions(Context context) {
        boolean isGranted = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            isGranted = connectivityManager.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED;
        }
        Log.i(TAG, "checkBackgroundDataTransferRestrictions: " + isGranted);
        return isGranted;
    }
}
