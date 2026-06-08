package org.fptn.vpn.services.speedtest;

import com.google.gson.Gson;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.websocket.LoginRequest;
import org.fptn.vpn.services.websocket.NativeHttpsClientImpl;
import org.fptn.vpn.services.websocket.NativeResponse;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class NativeLoginTask implements Callable<SpeedTestResult> {
    private static final String LOGIN_URL = "/api/v1/login";
    private static final int TIMEOUT = 10;

    private final ServerEntity serverEntity;
    private final NativeHttpsClientImpl nativeHttpsClient;

    public NativeLoginTask(ServerEntity serverEntity, String sniHost, BypassCensorshipMethod censorshipStrategy, SniSpoofingMode sniSpoofingMode) {
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
    public SpeedTestResult call() throws Exception {
        String requestBody = new Gson().toJson(new LoginRequest(serverEntity.getUsername(), serverEntity.getPassword()));
        NativeResponse response = nativeHttpsClient.Post(LOGIN_URL, requestBody, TIMEOUT);
        if (response != null && response.code == 200) {
            JSONObject json = new JSONObject(response.body);
            String accessToken = json.getString("access_token");
            return new SpeedTestResult(serverEntity, accessToken);
        }
        throw new Exception("Login failed for " + serverEntity.getHost() + " [code=" + (response != null ? response.code : -1) + "]");
    }
}
