package org.fptn.vpn.services.speedtest;

import android.util.Log;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
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

    public NativeSpeedTestTask(ServerEntity serverEntity, String sniHost, BypassCensorshipMethod censorshipStrategy) {
        this.serverEntity = serverEntity;
        this.nativeHttpsClient = new NativeHttpsClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                serverEntity.getMd5ServerFingerprint(),
                sniHost,
                censorshipStrategy
        );
    }

    @Override
    public NativeSpeedTestResult call() throws PVNClientException {
        Instant start = Instant.now();
        NativeResponse response = nativeHttpsClient.Get(GET_FILE_PATH, TIMEOUT);
        if (response.code == 200) {
            Instant end = Instant.now();
            long durationsMillis = Duration.between(start, end).toMillis();
            return new NativeSpeedTestResult(serverEntity, durationsMillis);
        } else {
            throw new PVNClientException(response.errorMessage);
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + ": " + serverEntity.getName();
    }
}
