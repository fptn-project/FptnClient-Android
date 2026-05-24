package org.fptn.vpn.services.snichecker;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;


public class SniChecker {
    private final String TAG = getClass().getSimpleName();
    private final ServerEntity selectedServer;
    private final BypassCensorshipMethod bypassCensorshipMethod;
    private long nativeHandle = 0;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public SniChecker(ServerEntity selectedServer, BypassCensorshipMethod bypassCensorshipMethod) {
        this.selectedServer = selectedServer;
        this.bypassCensorshipMethod = bypassCensorshipMethod;
        this.nativeHandle = nativeCreate(
                selectedServer.getHost(),
                selectedServer.getPort(),
                selectedServer.getMd5ServerFingerprint(),
                bypassCensorshipMethod.name()
        );
    }

    public boolean checkSni(String sni) {
        XLog.tag(TAG).d("checkSni: " + sni);

        if (nativeHandle == 0) {
            XLog.tag(TAG).e("Native handle is null");
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
