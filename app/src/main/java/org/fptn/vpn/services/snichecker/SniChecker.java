package org.fptn.vpn.services.snichecker;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;


public class SniChecker {
    private final String TAG = getClass().getSimpleName();
    private final ServerEntity selectedServer;
    private long nativeHandle = 0;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public SniChecker(ServerEntity selectedServer, BypassCensorshipMethod bypassCensorshipMethod, SniSpoofingMode sniSpoofingMode) {
        this.selectedServer = selectedServer;
        String strategyName = "SNI";
        if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY && sniSpoofingMode == SniSpoofingMode.SNI) {
            strategyName = "SNI";
        } else if (bypassCensorshipMethod == BypassCensorshipMethod.TLS_OBFUSCATION) {
            strategyName = "OBFUSCATION";
        } else if (bypassCensorshipMethod == BypassCensorshipMethod.SNI_REALITY) {
            strategyName = sniSpoofingMode.toString().replace('_', '-');
        }
        this.nativeHandle = nativeCreate(
                selectedServer.getHost(),
                selectedServer.getPort(),
                selectedServer.getMd5ServerFingerprint(),
                strategyName
        );
    }

    public boolean checkSni(String sni) {
        XLog.tag(TAG).d("Checking SNI [sni=%s]", sni);

        if (nativeHandle == 0) {
            XLog.tag(TAG).e("Cannot check SNI — native handle is null [sni=%s]", sni);
            return false;
        }
        return nativeCheckSni(nativeHandle, sni);
    }

    public void close() {
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    // Native methods
    private native long nativeCreate(String host, int port, String md5Fingerprint, String censorshipStrategy);

    private native boolean nativeCheckSni(long nativeHandle, String sni);

    private native void nativeDestroy(long nativeHandle);
}
