package org.fptn.vpn.utils;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;

public class NetworkUtils {
    private static final String TAG = NetworkUtils.class.getSimpleName();

    public static final String UNKNOWN_IP = "UNKNOWN";

    public static String getCurrentIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                while (inetAddressEnumeration.hasMoreElements()) {
                    InetAddress inetAddress = inetAddressEnumeration.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) { //!inetAddress.getHostAddress().contains(":")
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "getCurrentIPAddress() error!", e);
        }

        return UNKNOWN_IP;
    }

    public static NetworkType getActiveNetworkType(ConnectivityManager connectivityManager) {
        return Optional.ofNullable(connectivityManager.getActiveNetwork())
                .map(connectivityManager::getNetworkCapabilities)
                .map(NetworkUtils::getNetworkType)
                .orElse(NetworkType.UNKNOWN);
    }


    public static NetworkRequest createNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
//                .addTransportType(NetworkCapabilities.TRANSPORT_USB)
//                .addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE)
//                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
//                .addTransportType(NetworkCapabilities.TRANSPORT_LOWPAN)
//                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .build();
    }

    public static NetworkType getNetworkType(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.CELLULAR;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkType.ETHERNET;
        } else {
            return NetworkType.UNKNOWN;
        }
    }

    public static boolean isOnline(ConnectivityManager connectivityManager) {
        return Optional.ofNullable(connectivityManager.getActiveNetwork())
                .map(connectivityManager::getNetworkCapabilities)
                .map(networkCapabilities -> networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .orElse(false);
    }

}
