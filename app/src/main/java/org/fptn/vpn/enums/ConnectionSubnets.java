package org.fptn.vpn.enums;

import android.net.IpPrefix;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.Getter;

@Getter
public enum ConnectionSubnets {
    // todo: fix IPv6
    TUN_ADDRESS("10.10.0.1", 32, "::::", 128),
    TUN_INTERFACE_SUBNET("10.10.0.0", 16, "::::", 128),
    FPTN_SUBNET("172.16.0.0", 12, "::::", 128),
    LOCAL_SUBNET("192.168.0.0", 16, "::::", 128),
    ALL_SUBNET("0.0.0.0", 0, "::::", 128),

    // todo: rename me! STAS WHAT IS THIS ADDRESS?
    HZ_WHAT_IS_THIS_IP("fd00::1", 32, "::::", 126);

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
        return new IpPrefix(InetAddress.getByName(ipV4Address), v4prefix);
    }

    public String getAsIpV4PrefixAsString() {
        return ipV4Address + "/" + v4prefix;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public IpPrefix getAsIpV6Prefix() throws UnknownHostException {
        return new IpPrefix(InetAddress.getByName(ipV6Address), v6prefix);
    }

    public String getAsIpV6PrefixAsString() {
        return ipV6Address + "/" + v6prefix;
    }
}
