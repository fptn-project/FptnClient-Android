package org.fptn.vpn.services.tile;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.fptn.vpn.R;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.CustomVpnService;

import lombok.Getter;

public class FptnTileService extends TileService {
    private static final String TAG = FptnTileService.class.getSimpleName();

    @Getter
    private static final MutableLiveData<ConnectionState> serviceStateMutableLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);

    private Observer<ConnectionState> serviceStateObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "FptnTileService.onCreate()");
        serviceStateObserver = connectionState -> updateTile();
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "FptnTileService.onDestroy()");
        if (serviceStateObserver != null) {
            serviceStateMutableLiveData.removeObserver(serviceStateObserver);
            serviceStateObserver = null;
        }
    }

    @Override
    public void onClick() {
        Log.i(TAG, "FptnTileService.onClick()");

        ConnectionState connectionState = serviceStateMutableLiveData.getValue();
        if (connectionState != null && connectionState.isActiveState()) {
            CustomVpnService.startToDisconnect(this);
        } else {
            CustomVpnService.startToConnect(this, FptnServerDto.AUTO);
        }
    }

    @Override
    public void onStartListening() {
        Log.i(TAG, "FptnTileService.onStartListening()");
        updateTile();
    }

    private void updateTile() {
        Log.d(TAG, "updateTile()");
        Tile tile = getQsTile();
        if (tile != null) {
            // Update tile state
            ConnectionState connectionState = serviceStateMutableLiveData.getValue();
            if (connectionState != null && connectionState.isActiveState()) {
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel("FPTN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("ON");
                }
                tile.setContentDescription("Connected to ESTONIA");
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_shield_on_24));
                tile.updateTile();
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel("FPTN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle("OFF");
                }
                tile.setContentDescription("Disconnected");
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_shield_off_24));
                tile.updateTile();
            }
        }
    }
}
