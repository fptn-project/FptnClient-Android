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

package org.fptn.vpn.services.speedtest;

import android.os.Build;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.websocket.NativeHttpsClientImpl;
import org.fptn.vpn.services.websocket.NativeResponse;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import lombok.Getter;

public class NativeSpeedTestTask implements Callable<NativeSpeedTestResult> {
    private static final String GET_FILE_PATH = "/api/v1/test/file.bin";
    private static final int TIMEOUT = 10;

    @Getter
    private final ServerEntity serverEntity;
    private final NativeHttpsClientImpl nativeHttpsClient;

    public NativeSpeedTestTask(ServerEntity serverEntity, String sniHost, BypassCensorshipMethod censorshipStrategy, SniSpoofingMode sniSpoofingMode) {
        this.serverEntity = serverEntity;
        this.nativeHttpsClient = new NativeHttpsClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                serverEntity.getMd5ServerFingerprint(),
                sniHost,
                censorshipStrategy,
                sniSpoofingMode
        );
    }

    @Override
    public NativeSpeedTestResult call() throws PVNClientException {
        Instant start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Instant.now() : null;
        long startMillis = System.currentTimeMillis();
        NativeResponse response = nativeHttpsClient.Get(GET_FILE_PATH, TIMEOUT);
        if (response.code == 200) {
            long durationsMillis;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant end = Instant.now();
                durationsMillis = Duration.between(start, end).toMillis();
            } else {
                durationsMillis = System.currentTimeMillis() - startMillis;
            }
            return new NativeSpeedTestResult(serverEntity, durationsMillis);
        } else {
            throw new PVNClientException(response.errorMessage);
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + ": " + serverEntity.getName();
    }
}
