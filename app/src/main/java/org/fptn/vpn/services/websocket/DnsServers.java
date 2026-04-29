package org.fptn.vpn.services.websocket;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DnsServers {
    @SerializedName("dns")
    private String ipv4;
    @SerializedName("dns_ipv6")
    private String ipv6;
}
