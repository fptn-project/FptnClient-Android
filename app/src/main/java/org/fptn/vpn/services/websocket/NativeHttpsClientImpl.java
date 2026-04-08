package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.enums.BypassCensorshipMethod;

public class NativeHttpsClientImpl {
    private static final String TAG = NativeHttpsClientImpl.class.getName();

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public NativeHttpsClientImpl(String serverIP,
                                 int serverPort,
                                 String md5Fingerprint,
                                 String sni,
                                 BypassCensorshipMethod censorshipStrategy) {
        String censorshipStrategyName = "SNI";
        if (censorshipStrategy == BypassCensorshipMethod.TLS_OBFUSCATION) {
            censorshipStrategyName = "OBFUSCATION";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY) {  // deprecated
            censorshipStrategyName = "SNI-REALITY";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_CHROME_146) {
            censorshipStrategyName = "SNI-REALITY-CHROME-146";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_FIREFOX_149) {
            censorshipStrategyName = "SNI-REALITY-FIREFOX-149";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_YANDEX_26) {
            censorshipStrategyName = "SNI-REALITY-YANDEX-26";
        } else if (censorshipStrategy == BypassCensorshipMethod.SNI_REALITY_YANDEX_25) {
            censorshipStrategyName = "SNI-REALITY-YANDEX-25";
        }
        this.nativeHandle = nativeCreate(
                serverIP,
                serverPort,
                sni,
                md5Fingerprint,
                censorshipStrategyName
        );
    }

    public NativeResponse Get(String url, int timeout) {
        return nativeGet(nativeHandle, url, timeout);
    }

    public NativeResponse Post(String url, String body, int timeout) {
        return nativePost(nativeHandle, url, body, timeout);
    }

    public synchronized void release() {
        Log.d(TAG, "NativeHttpsClientImpl.release()");
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String sni,
                                     String expected_md5_fingerprint,
                                     String censorship_strategy_name);

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "NativeHttpsClientImpl.finalize()");
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    private native void nativeDestroy(long nativeHandle);

    private native NativeResponse nativeGet(long nativeHandle, String url, int timeout);

    private native NativeResponse nativePost(long nativeHandle, String url, String requestBody, int timeout);
}
