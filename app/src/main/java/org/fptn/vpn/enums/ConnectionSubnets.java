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

package org.fptn.vpn.enums;

import android.net.IpPrefix;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import lombok.Getter;

@Getter
public enum ConnectionSubnets {
    LOCAL_TUN_INTERFACE_SUBNET("10.10.0.0", 16, "fd00::", 64),
    FPTN_SERVER_SUBNET("172.16.0.0", 12, "fc00:1::", 64),
    LOCAL_SUBNET("192.168.0.0", 16, "fe80::", 10),
    TUN_ADDRESS("10.10.0.1", 32, "fd00::1", 128),
    ALL_SUBNET("0.0.0.0", 0, "::", 0);

    public static final int IP_V4_PREFIX_LENGTH = 32;
    public static final int IP_V6_PREFIX_LENGTH = 128;

    private final String ipV4Address;
    private final int v4prefix;

    private final String ipV6Address;
    private final int v6prefix;

    ConnectionSubnets(String ipV4Address, int v4prefix, String ipV6Address, int v6prefix) {
        this.ipV4Address = ipV4Address;
        this.v4prefix = v4prefix;
        this.ipV6Address = ipV6Address;
        this.v6prefix = v6prefix;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public IpPrefix getAsIpV4Prefix() throws UnknownHostException {
        return new IpPrefix(Inet4Address.getByName(ipV4Address), v4prefix);
    }

    public String getAsIpV4PrefixAsString() {
        return ipV4Address + "/" + v4prefix;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public IpPrefix getAsIpV6Prefix() throws UnknownHostException {
        return new IpPrefix(Inet6Address.getByName(ipV6Address), v6prefix);
    }

    public String getAsIpV6PrefixAsString() {
        return ipV6Address + "/" + v6prefix;
    }

}
