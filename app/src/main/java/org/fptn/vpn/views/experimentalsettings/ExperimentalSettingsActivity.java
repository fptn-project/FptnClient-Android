package org.fptn.vpn.views.experimentalsettings;

import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.R;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.SharedPrefUtils;

public class ExperimentalSettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private SwitchCompat switchNetworkType;
    private SwitchCompat switchIPAddress;
    private SeekBar seekBarAttemptsCount;
    private SeekBar seekBarDelayBetween;
    private SwitchCompat resetServerAfterDisconnectSwitch;
    private SwitchCompat resetServerAfterDisconnectOnException;
    private SwitchCompat xiaomiOptimizationSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.experimental_settings_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        switchNetworkType = findViewById(R.id.reconnect_on_change_network_type_switch);
        switchNetworkType.setChecked(SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this));

        switchIPAddress = findViewById(R.id.reconnect_on_change_ip_address_switch);
        switchIPAddress.setChecked(SharedPrefUtils.getReconnectOnChangeIPEnabled(this));

        // Reconnects attempts count
        seekBarAttemptsCount = findViewById(R.id.seekBarAttemptsCount);
        TextView textViewAttemptsCount = findViewById(R.id.textViewAttemptsCount);
        seekBarAttemptsCount.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 3) {
                    textViewAttemptsCount.setText("∞");
                } else {
                    String format = getString(R.string.reconnect_attempts_text);
                    textViewAttemptsCount.setText(String.format(format, progress * 5));
                }
            }
        });

        seekBarAttemptsCount.setProgress(0);
        int reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        if (reconnectAttemptsCount == Integer.MAX_VALUE) {
            seekBarAttemptsCount.setProgress(3);
        } else {
            seekBarAttemptsCount.setProgress(reconnectAttemptsCount / 5);
        }

        // Reconnects delay between in seconds
        seekBarDelayBetween = findViewById(R.id.seekBarDelayBetween);
        TextView textViewDelayBetween = findViewById(R.id.textViewDelayBetween);
        seekBarDelayBetween.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String format = getString(R.string.delay_between_attempts_seconds);
                textViewDelayBetween.setText(String.format(format, progress + 1));
            }
        });

        int delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(this);
        seekBarDelayBetween.setProgress(0);
        seekBarDelayBetween.setProgress(delayBetweenReconnect - 1);

        // Quick tile request
        View tileButtonLayout = findViewById(R.id.tile_layout);
        Button buttonRequestTile = findViewById(R.id.quick_settings_tile_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buttonRequestTile.setVisibility(View.VISIBLE);
            buttonRequestTile.setEnabled(true);
            buttonRequestTile.setOnClickListener(v -> requestQuickSettingsTile(buttonRequestTile));

            tileButtonLayout.setVisibility(View.VISIBLE);
        } else {
            buttonRequestTile.setVisibility(View.GONE); // Use GONE to free up layout space

            tileButtonLayout.setVisibility(View.GONE);
        }

        // Reset selected server on disconnect
        resetServerAfterDisconnectSwitch = findViewById(R.id.reset_selected_server_after_disconnect_switch);
        resetServerAfterDisconnectSwitch.setChecked(SharedPrefUtils.getResetSelectedServerEnabled(this));

        // Reset selected server on disconnect with exception
        resetServerAfterDisconnectOnException = findViewById(R.id.reset_selected_server_after_disconnect_with_exception);
        resetServerAfterDisconnectOnException.setChecked(SharedPrefUtils.getResetSelectedServerEnabled(this));

        resetServerAfterDisconnectOnException.setChecked(SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(this));
        updateExceptionVisibility(resetServerAfterDisconnectSwitch.isChecked());

        // Toggle visibility on change
        resetServerAfterDisconnectSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateExceptionVisibility(isChecked));

        // Xiaomi/HyperOS optimization
        xiaomiOptimizationSwitch = findViewById(R.id.xiaomi_optimization_switch);
        xiaomiOptimizationSwitch.setChecked(SharedPrefUtils.getXiaomiOptimizationEnabled(this));


        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            XLog.tag(TAG).d("Cancel button clicked");
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveAndFinish());
    }

    private void updateExceptionVisibility(boolean isVisible) {
        resetServerAfterDisconnectOnException.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestQuickSettingsTile(Button buttonRequestTile) {
        StatusBarManager statusBarManager = getSystemService(StatusBarManager.class);
        if (statusBarManager == null) return;

        // Disable button to prevent double-clicks
        buttonRequestTile.setEnabled(false);

        try {
            ComponentName componentName = new ComponentName(this, FptnTileService.class);
            String label = getString(R.string.app_name); // Or specific tile label
            Icon icon = Icon.createWithResource(this, R.drawable.ic_logo);
            statusBarManager.requestAddTileService(
                    componentName,
                    label,
                    icon,
                    getMainExecutor(),
                    resultCode -> handleTileRequestResult(resultCode, buttonRequestTile)
            );
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to request tile addition", e);
            Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show();
            buttonRequestTile.setEnabled(true);
        }
    }

    private void handleTileRequestResult(int resultCode, Button button) {
        // Re-enable button unless the tile was successfully added/exists
        boolean shouldKeepDisabled = false;

        switch (resultCode) {
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED:
                XLog.tag(TAG).d("Tile already added successfully.");
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
                button.setBackgroundResource(R.drawable.round_back_secondary_cancel_100);
                shouldKeepDisabled = true;
                break;

            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED:
                XLog.tag(TAG).d("Tile added successfully.");
                Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show();
                shouldKeepDisabled = true;
                break;

            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED:
                XLog.tag(TAG).d("User cancelled the request or it failed.");
                button.setEnabled(true);
                break;

            default:
                XLog.tag(TAG).d("Unknown result code: " + resultCode);
                button.setEnabled(true);
                break;
        }

        if (shouldKeepDisabled) {
            button.setEnabled(false);
            button.setAlpha(0.5f); // Visual cue that it's no longer needed
        }
    }

    private void saveAndFinish() {
        XLog.tag(TAG).d("Save button clicked");

        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, switchNetworkType.isChecked());
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, switchIPAddress.isChecked());
        SharedPrefUtils.saveResetSelectedServerEnabled(this, resetServerAfterDisconnectSwitch.isChecked());
        SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(this, resetServerAfterDisconnectOnException.isChecked());
        SharedPrefUtils.saveXiaomiOptimizationEnabled(this, xiaomiOptimizationSwitch.isChecked());

        int attemptsCountProgress = seekBarAttemptsCount.getProgress();
        if (attemptsCountProgress == 3) {
            SharedPrefUtils.saveReconnectAttemptsCount(this, Integer.MAX_VALUE);
        } else {
            SharedPrefUtils.saveReconnectAttemptsCount(this, attemptsCountProgress * 5);
        }

        int delayBetweenProgress = seekBarDelayBetween.getProgress();
        SharedPrefUtils.saveDelayBetweenReconnect(this, delayBetweenProgress + 1);

        finish();
    }

    // Helper to reduce boilerplate for SeekBars
    private abstract static class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}

