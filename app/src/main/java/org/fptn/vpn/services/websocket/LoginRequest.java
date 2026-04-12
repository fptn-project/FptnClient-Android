package org.fptn.vpn.services.websocket;

import lombok.Data;

@Data
public class LoginRequest {
    private final String username;
    private final String password;
}
