package org.fptn.vpn.utils;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;

public class NetworkUtils {

    public static final String UNKNOWN_IP = "UNKNOWN";

    public static String getCurrentIPAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
            while (inetAddressEnumeration.hasMoreElements()) {
                InetAddress inetAddress = inetAddressEnumeration.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return UNKNOWN_IP;
    }

    public static NetworkRequest createNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
//                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
//                .addTransportType(NetworkCapabilities.TRANSPORT_USB)
//                .addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE)
//                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
//                .addTransportType(NetworkCapabilities.TRANSPORT_LOWPAN)
//                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .build();
    }


    public static boolean isOnline(ConnectivityManager connectivityManager) {
        return Optional.ofNullable(connectivityManager.getActiveNetwork())
                .map(connectivityManager::getNetworkCapabilities)
                .map(networkCapabilities -> networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .orElse(false);
    }

}
