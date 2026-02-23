package org.fptn.vpn.utils.token;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class Token {
    Integer version;
    @JsonProperty("service_name")
    String serviceName;
    String username;
    String password;

    List<ServerFromToken> servers;
    @JsonProperty("censored_zone_servers")
    List<ServerFromToken> censoredServers;
}
