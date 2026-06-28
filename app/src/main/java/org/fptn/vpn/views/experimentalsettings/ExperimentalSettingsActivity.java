/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.views.experimentalsettings;

import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

    private static final int[] ATTEMPTS_COUNT_VALUES = {5, 15, 35, Integer.MAX_VALUE};
    private static final int[] FALLBACK_THRESHOLD_VALUES = {3, 6, 10, 15};

    private SwitchCompat customDnsSwitch;
    private EditText customDnsInput;

    private SwitchCompat switchNetworkType;
    private SwitchCompat switchIPAddress;
    private SeekBar seekBarAttemptsCount;
    private SeekBar seekBarDelayBetween;
    private SwitchCompat resetServerAfterDisconnectSwitch;
    private SwitchCompat resetServerAfterDisconnectOnException;
    private SwitchCompat autoFallbackSwitch;
    private View autoFallbackThresholdLayout;
    private SeekBar seekBarFallbackThreshold;
    private SwitchCompat adBlockSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.experimental_settings_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        customDnsSwitch = findViewById(R.id.custom_dns_switch);
        customDnsInput = findViewById(R.id.custom_dns_input);
        customDnsInput.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        customDnsSwitch.setChecked(SharedPrefUtils.getCustomDnsEnabled(this));
        customDnsInput.setText(SharedPrefUtils.getCustomDnsIpv4(this));
        updateCustomDnsInputVisibility(customDnsSwitch.isChecked());
        customDnsSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateCustomDnsInputVisibility(isChecked));

        adBlockSwitch = findViewById(R.id.ad_block_switch);
        adBlockSwitch.setChecked(SharedPrefUtils.getAdBlockEnabled(this));

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
                int value = ATTEMPTS_COUNT_VALUES[progress];
                if (value == Integer.MAX_VALUE) {
                    textViewAttemptsCount.setText("∞");
                } else {
                    String format = getString(R.string.reconnect_attempts_text);
                    textViewAttemptsCount.setText(String.format(format, value));
                }
            }
        });

        int reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        int progressToSet = 2; // default to 35
        for (int i = 0; i < ATTEMPTS_COUNT_VALUES.length; i++) {
            if (ATTEMPTS_COUNT_VALUES[i] >= reconnectAttemptsCount) {
                progressToSet = i;
                break;
            }
        }
        seekBarAttemptsCount.setProgress(progressToSet);

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

        // Auto-fallback to all servers
        autoFallbackSwitch = findViewById(R.id.auto_fallback_switch);
        autoFallbackSwitch.setChecked(SharedPrefUtils.getAutoFallbackEnabled(this));

        autoFallbackThresholdLayout = findViewById(R.id.auto_fallback_threshold_layout);
        seekBarFallbackThreshold = findViewById(R.id.seekBarFallbackThreshold);
        TextView textViewFallbackThreshold = findViewById(R.id.textViewFallbackThreshold);
        seekBarFallbackThreshold.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String format = getString(R.string.auto_fallback_threshold_value);
                textViewFallbackThreshold.setText(String.format(format, FALLBACK_THRESHOLD_VALUES[progress]));
            }
        });

        int savedThreshold = SharedPrefUtils.getAutoFallbackThreshold(this);
        int fallbackProgressToSet = 1; // default to 6
        for (int i = 0; i < FALLBACK_THRESHOLD_VALUES.length; i++) {
            if (FALLBACK_THRESHOLD_VALUES[i] >= savedThreshold) {
                fallbackProgressToSet = i;
                break;
            }
        }
        seekBarFallbackThreshold.setProgress(fallbackProgressToSet);
        updateAutoFallbackThresholdVisibility(autoFallbackSwitch.isChecked());
        autoFallbackSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateAutoFallbackThresholdVisibility(isChecked));

        // Save and Cancel buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            XLog.tag(TAG).i("Experimental settings cancelled");
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveAndFinish());
    }

    private void updateCustomDnsInputVisibility(boolean isEnabled) {
        customDnsInput.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateExceptionVisibility(boolean isVisible) {
        resetServerAfterDisconnectOnException.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void updateAutoFallbackThresholdVisibility(boolean isEnabled) {
        autoFallbackThresholdLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
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
            XLog.tag(TAG).e("Failed to request quick settings tile addition: %s", e.getMessage());
            Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT).show();
            buttonRequestTile.setEnabled(true);
        }
    }

    private void handleTileRequestResult(int resultCode, Button button) {
        // Re-enable button unless the tile was successfully added/exists
        boolean shouldKeepDisabled = false;

        switch (resultCode) {
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED:
                XLog.tag(TAG).i("Quick settings tile already present");
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
                button.setBackgroundResource(R.drawable.round_back_secondary_cancel_100);
                shouldKeepDisabled = true;
                break;

            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED:
                XLog.tag(TAG).i("Quick settings tile added successfully");
                Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT).show();
                shouldKeepDisabled = true;
                break;

            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED:
                XLog.tag(TAG).w("Quick settings tile request was declined");
                button.setEnabled(true);
                break;

            default:
                XLog.tag(TAG).w("Quick settings tile request returned unexpected result [code=%d]", resultCode);
                button.setEnabled(true);
                break;
        }

        if (shouldKeepDisabled) {
            button.setEnabled(false);
            button.setAlpha(0.5f); // Visual cue that it's no longer needed
        }
    }

    private void saveAndFinish() {
        if (customDnsSwitch.isChecked()) {
            String dnsValue = customDnsInput.getText().toString().trim();
            if (!isValidDnsAddress(dnsValue)) {
                Toast.makeText(this, R.string.custom_dns_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPrefUtils.saveCustomDnsIpv4(this, dnsValue);
        }
        SharedPrefUtils.saveCustomDnsEnabled(this, customDnsSwitch.isChecked());

        XLog.tag(TAG).i("Experimental settings saved [watchNetwork=%b, watchIP=%b, attempts=%d, delay=%ds]", switchNetworkType.isChecked(), switchIPAddress.isChecked(), seekBarAttemptsCount.getProgress(), seekBarDelayBetween.getProgress() + 1);

        SharedPrefUtils.saveAdBlockEnabled(this, adBlockSwitch.isChecked());
        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, switchNetworkType.isChecked());
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, switchIPAddress.isChecked());
        SharedPrefUtils.saveResetSelectedServerEnabled(this, resetServerAfterDisconnectSwitch.isChecked());
        SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(this, resetServerAfterDisconnectOnException.isChecked());

        int attemptsCountProgress = seekBarAttemptsCount.getProgress();
        SharedPrefUtils.saveReconnectAttemptsCount(this, ATTEMPTS_COUNT_VALUES[attemptsCountProgress]);

        int delayBetweenProgress = seekBarDelayBetween.getProgress();
        SharedPrefUtils.saveDelayBetweenReconnect(this, delayBetweenProgress + 1);

        SharedPrefUtils.saveAutoFallbackEnabled(this, autoFallbackSwitch.isChecked());
        SharedPrefUtils.saveAutoFallbackThreshold(this, FALLBACK_THRESHOLD_VALUES[seekBarFallbackThreshold.getProgress()]);

        finish();
    }

    private static boolean isValidDnsAddress(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String[] parts = value.trim().split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
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
