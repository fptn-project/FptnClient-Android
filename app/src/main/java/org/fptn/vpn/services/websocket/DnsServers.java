package org.fptn.vpn.services.websocket;

public class DnsServers {
    private final String ipv4;
    private final String ipv6;

    public DnsServers(String ipv4, String ipv6) {
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
    }

    public String getIpv4() { return ipv4; }
    public String getIpv6() { return ipv6; }
}
