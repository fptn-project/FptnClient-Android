package org.fptn.vpn.views.speedtest;

import org.fptn.vpn.database.model.FptnServerDto;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class NativeSpeedTestResult {
    FptnServerDto fptnServerDto;
    long durationsMillis;
}
