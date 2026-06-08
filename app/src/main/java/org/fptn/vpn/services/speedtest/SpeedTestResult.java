package org.fptn.vpn.services.speedtest;

import org.fptn.vpn.database.entity.ServerEntity;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class SpeedTestResult {
    ServerEntity serverEntity;
    String accessToken;
}
