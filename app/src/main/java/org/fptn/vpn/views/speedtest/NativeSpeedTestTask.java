package org.fptn.vpn.views.speedtest;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
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
    private final FptnServerDto fptnServerDto;
    private final NativeHttpsClientImpl nativeHttpsClient;

    public NativeSpeedTestTask(FptnServerDto fptnServerDto, String sniHost) {
        this.fptnServerDto = fptnServerDto;
        this.nativeHttpsClient = new NativeHttpsClientImpl(
                fptnServerDto.host,
                fptnServerDto.port,
                sniHost,
                fptnServerDto.md5ServerFingerprint);
    }

    @Override
    public NativeSpeedTestResult call() throws PVNClientException {
        Instant start = Instant.now();
        Log.d(getTag(), "call() start test: " + fptnServerDto.name);
        NativeResponse response = nativeHttpsClient.Get(GET_FILE_PATH, TIMEOUT);
        if (response.code == 200) {
            Instant end = Instant.now();
            long durationsMillis = Duration.between(start, end).toMillis();
            Log.d(getTag(), "call() end test: " + fptnServerDto.name + " duration: " + durationsMillis + " ms");
            return new NativeSpeedTestResult(fptnServerDto, durationsMillis);
        } else {
            Log.d(getTag(), "call() end test: " + fptnServerDto.name + " error: " + response.errorMessage);
            throw new PVNClientException(response.errorMessage);
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + ": " + fptnServerDto.name;
    }
}
