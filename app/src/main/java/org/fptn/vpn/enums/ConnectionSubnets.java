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
    LOCAL_TUN_ADDRESS("10.10.0.1", 32, "fd00::1", 128),
    LOCAL_TUN_INTERFACE_SUBNET("10.10.0.0", 16, "fd00:::", 64),
    FPTN_SERVER_SUBNET("172.16.0.0", 12, "fc00:1::", 64),
    LOCAL_SUBNET("192.168.0.0", 16, "::::", 128),
    ALL_SUBNET("0.0.0.0", 0, "::::", 128);

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

    public String getIpV4Address() {
        return ipV4Address;
    }

    public String getIpV6Address() {
        return ipV6Address;
    }

    public int getIpV4Prefix() {
        return v4prefix;
    }

    public int getIpV6Prefix() {
        return v6prefix;
    }

}
