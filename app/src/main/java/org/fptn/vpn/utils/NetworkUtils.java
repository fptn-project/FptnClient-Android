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

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.enums.NetworkType;

import java.net.Inet4Address;
import java.net.Inet6Address;
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
                            && (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address)) {
                        XLog.tag(TAG).d("Current IP address [ip=%s, iface=%s]", inetAddress.getHostAddress(), networkInterface.getName());
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            XLog.tag(TAG).e("Failed to resolve local IP address: %s", e.getMessage());
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
