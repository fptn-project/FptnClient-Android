package org.fptn.vpn.services.websocket;

import android.util.Log;

import com.google.gson.Gson;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

public class WebSocketClientWrapper {
    private static final String DNS_URL = "/api/v1/dns";
    private static final String LOGIN_URL = "/api/v1/login";

    private final ServerEntity serverEntity;
    private final String tunAddressIPv4;
    private final String tunAddressIPv6;
    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final NativeHttpsClientImpl nativeHttpsClient;
    private final String sniHostName;
    private final BypassCensorshipMethod censorshipStrategy;

    private NativeWebSocketClientImpl nativeWebSocketClient;

    @Getter
    private boolean shutdown = false;

    public WebSocketClientWrapper(ServerEntity serverEntity,
                                  String tunAddressIPv4,
                                  String tunAddressIPv6,
                                  OnOpenCallback onOpenCallback,
                                  OnMessageReceivedCallback onMessageReceivedCallback,
                                  OnFailureCallback onFailureCallback,
                                  String sniHostName,
                                  BypassCensorshipMethod censorshipStrategy) {
        this.serverEntity = serverEntity;
        this.tunAddressIPv4 = tunAddressIPv4;
        this.tunAddressIPv6 = tunAddressIPv6;
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        // this is SNI spoofing
        this.sniHostName = sniHostName;
        this.censorshipStrategy = censorshipStrategy;

        this.nativeHttpsClient = new NativeHttpsClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                serverEntity.getMd5ServerFingerprint(),
                sniHostName,
                censorshipStrategy
        );
    }

    public synchronized void startWebSocket() throws PVNClientException, WebSocketAlreadyShutdownException {
        if (isShutdown()) {
            throw new WebSocketAlreadyShutdownException();
        }
        stopWebSocket();

        String accessToken = getAccessToken(); // maybe move to constructor if it not changed between connections?

        nativeWebSocketClient = new NativeWebSocketClientImpl(
                serverEntity.getHost(),
                serverEntity.getPort(),
                tunAddressIPv4,
                tunAddressIPv6,
                accessToken,
                serverEntity.getMd5ServerFingerprint(),
                onOpenCallback,
                onMessageReceivedCallback,
                onFailureCallback,
                sniHostName,
                censorshipStrategy
        );

        Log.d(getTag(), "startWebSocket() nativeWebSocketClient.start() Thread.id: " + Thread.currentThread().getId());
        nativeWebSocketClient.start();
    }

    public synchronized void stopWebSocket() {
        Log.d(getTag(), "stopWebSocket()");
        if (nativeWebSocketClient != null) {
            if (nativeWebSocketClient.isStarted()) {
                Log.d(getTag(), "stopWebSocket() nativeWebSocketClient.stop() Thread.id: " + Thread.currentThread().getId());
                nativeWebSocketClient.stop();
            }
            Log.d(getTag(), "stopWebSocket() nativeWebSocketClient.release() Thread.id: " + Thread.currentThread().getId());
            nativeWebSocketClient.release();
            nativeWebSocketClient = null;
        }
    }

    public void send(byte[] bytes, long length) {
        if (nativeWebSocketClient != null
                && nativeWebSocketClient.isStarted()
                && length > 0) {
            nativeWebSocketClient.send(bytes, length);
        } else {
            throw new RuntimeException("nativeWebSocketClient is null or not started");
        }
    }

    public synchronized void shutdown() {
        stopWebSocket();
        shutdown = true;
    }

    private String getAccessToken() throws PVNClientException {
        LoginRequest loginRequest = new LoginRequest(
                serverEntity.getUsername(),
                serverEntity.getPassword()
        );
        String requestBody = new Gson().toJson(loginRequest);
        NativeResponse response = nativeHttpsClient.Post(LOGIN_URL, requestBody, 15);
        if (response != null) {
            if (response.code == 200) {
                try {
                    JSONObject jsonResponse = new JSONObject(response.body);
                    String accessToken = jsonResponse.getString("access_token");
                    Log.i(getTag(), "Getting accessToken successful.");
                    return accessToken;
                } catch (JSONException e) {
                    Log.e(getTag(), "Some error occurs on parsing accessToken response: " + e);
                    throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
                }
            } else if (response.code == 401) {
                Log.e(getTag(), "Server return unsuccess response: " + response.errorMessage);
                throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR);
            } else {
                Log.e(getTag(), "Server return unsuccess response!");
                throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }

    public DnsServers getDnsServers() throws PVNClientException {
        NativeResponse response = nativeHttpsClient.Get(DNS_URL, 15);
        if (response != null && response.code == 200) {
            try {
                JSONObject jsonResponse = new JSONObject(response.body);
                String ipv4 = jsonResponse.getString("dns");
                String ipv6 = jsonResponse.getString("dns_ipv6");
                Log.i(getTag(), "DNS_IPv4: " + ipv4 + "  DNS_IPv6: " + ipv6);
                return new DnsServers(ipv4, ipv6);
            } catch (JSONException e) {
                Log.e(getTag(), "Error parsing DNS response: " + e);
                throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }


    private String getTag() {
        return this.getClass().getCanonicalName();
    }


}
