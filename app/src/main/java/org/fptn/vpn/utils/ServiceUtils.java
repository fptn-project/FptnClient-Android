package org.fptn.vpn.utils;

import android.content.pm.ServiceInfo;
import android.util.Log;

public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();

    public static final String NONE = "none";

    public static boolean isServiceForeground(int foregroundServiceType) {
        return !NONE.equals(foregroundServiceTypeToLabel(foregroundServiceType));
    }

    public static String foregroundServiceTypeToLabel(int type) {
        String foregroundServiceLabel = switch (type) {
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST -> "manifest";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC -> "dataSync";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK -> "mediaPlayback";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL -> "phoneCall";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION -> "location";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE -> "connectedDevice";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION -> "mediaProjection";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA -> "camera";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE -> "microphone";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH -> "health";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING -> "remoteMessaging";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED ->
                    "systemExempted"; // this type declared in android manifest
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE -> "shortService";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING -> "mediaProcessing";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE -> "specialUse";
            case ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE -> NONE; // it's not foreground service
            default -> "unknown"; // not exist
        };
        Log.d(TAG, "ForegroundServiceType: " + foregroundServiceLabel);
        return foregroundServiceLabel;
    }
}
