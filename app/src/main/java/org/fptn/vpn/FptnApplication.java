package org.fptn.vpn;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.fptn.vpn.services.CustomVpnService;

public class FptnApplication extends Application {
    private final String TAG = this.getClass().getSimpleName();

    //for service binding
    private ServiceConnection connection;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate - App started. Thread.Id: " + Thread.currentThread().getId());

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "onServiceConnected: " + name);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected: " + name);
            }
        };
        bindService(new Intent(this, CustomVpnService.class).setAction("ON_BIND"), connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Log.d(TAG, "onTerminate - App terminated");

        unbindService(connection);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Log.d(TAG, "onLowMemory - Low memory detected");
    }

}
