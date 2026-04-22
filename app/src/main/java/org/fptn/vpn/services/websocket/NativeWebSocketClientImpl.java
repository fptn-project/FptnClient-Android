package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.concurrent.atomic.AtomicInteger;

public class NativeWebSocketClientImpl {
    private static final String TAG = NativeWebSocketClientImpl.class.getName();

    static {
        System.loadLibrary("fptn_native_lib");
    }

    /* Only for debug */
    private static final AtomicInteger SERIAL_NUM = new AtomicInteger(555);
    private final int serialNum;

    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;

    private long nativeHandle;

    public NativeWebSocketClientImpl(
            String host,
            int port,
            String tunAddress,
            String accessToken,
            String md5ServerFingerprint,
            OnOpenCallback onOpenCallback,
            OnMessageReceivedCallback onMessageReceivedCallback,
            OnFailureCallback onFailureCallback,
            String sniHostName,
            BypassCensorshipMethod censorshipStrategy) throws PVNClientException {
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        String censorshipStrategyName = "SNI-REALITY-YANDEX-25";
        if (censorshipStrategy == BypassCensorshipMethod.TLS_OBFUSCATION) {
            censorshipStrategyName = "OBFUSCATION";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY) {  // deprecated
            censorshipStrategyName = "SNI-REALITY";
        }
        /* Chrome */
        else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_CHROME_147) {
            censorshipStrategyName = "SNI-REALITY-CHROME-147";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_CHROME_146) {
            censorshipStrategyName = "SNI-REALITY-CHROME-146";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_CHROME_145) {
            censorshipStrategyName = "SNI-REALITY-CHROME-145";
        }
        /* Firefox */
        else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_FIREFOX_149) {
            censorshipStrategyName = "SNI-REALITY-FIREFOX-149";
        }
        /* Yandex */
        else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_YANDEX_26) {
            censorshipStrategyName = "SNI-REALITY-YANDEX-26";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_YANDEX_25) {
            censorshipStrategyName = "SNI-REALITY-YANDEX-25";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_YANDEX_24) {
            censorshipStrategyName = "SNI-REALITY-YANDEX-24";
        }
        /* Safari */
        else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_SAFARI_26) {
            censorshipStrategyName = "SNI-REALITY-SAFARI-26";
        }

        this.nativeHandle = nativeCreate(
                host,
                port,
                tunAddress,
                sniHostName,
                accessToken,
                md5ServerFingerprint,
                censorshipStrategyName
        );

        this.serialNum = SERIAL_NUM.getAndIncrement();

        if (this.nativeHandle == 0L) {
            throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
        }
    }

    public void start() {
        Log.d(TAG, "NativeWebSocketClientImpl.start() Thread.id: " + Thread.currentThread().getId() + " serialNum: " + serialNum);
        if (!nativeIsStarted(nativeHandle)) {
            nativeRun(nativeHandle);
        }
    }

    public void stop() {
        Log.d(TAG, "NativeWebSocketClientImpl.stop() Thread.id: " + Thread.currentThread().getId() + " serialNum: " + serialNum);
        if (nativeIsStarted(nativeHandle)) {
            nativeStop(nativeHandle);
        }
    }

    public boolean isStarted() {
        return nativeIsStarted(nativeHandle);
    }

    public void send(byte[] data) {
        if (nativeHandle != 0L && nativeIsStarted(nativeHandle)) {
            nativeSend(nativeHandle, data);
        }
    }

    public synchronized void release() {
        Log.d(TAG, "NativeWebSocketClientImpl.release() Thread.id: " + Thread.currentThread().getId() + " serialNum: " + serialNum);
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "NativeWebSocketClientImpl.finalize() Thread.id: " + Thread.currentThread().getId() + " serialNum: " + serialNum);
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    public void onOpenImpl() {
        Log.d(TAG, "NativeWebSocketClientImpl.onOpenImpl():start()" + " serialNum: " + serialNum);
        if (this.onOpenCallback != null) {
            this.onOpenCallback.onOpen();
        }
        Log.d(TAG, "NativeWebSocketClientImpl.onOpenImpl():end()");
    }

    public void onFailureImpl() {
        Log.d(TAG, "NativeWebSocketClientImpl.onFailureImpl():start()" + " serialNum: " + serialNum);
        if (this.onFailureCallback != null) {
            this.onFailureCallback.onFailure();
        }
        Log.d(TAG, "NativeWebSocketClientImpl.onFailureImpl():end()");
    }

    public void onMessageImpl(byte[] msg) {
        if (this.onMessageReceivedCallback != null) {
            this.onMessageReceivedCallback.onMessageReceived(msg);
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String tun_ipv4,
                                     String sni,
                                     String access_token,
                                     String expected_md5_fingerprint,
                                     String censorship_strategy_name);

    private native void nativeDestroy(long nativeHandle);

    private native boolean nativeRun(long nativeHandle);

    private native boolean nativeStop(long nativeHandle);

    private native boolean nativeSend(long nativeHandle, byte[] data);

    private native boolean nativeIsStarted(long nativeHandle);
}
