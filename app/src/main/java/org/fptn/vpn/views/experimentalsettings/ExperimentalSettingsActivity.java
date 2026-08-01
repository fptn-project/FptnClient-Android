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
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;

public class ExperimentalSettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private static final int[] ATTEMPTS_COUNT_VALUES = {5, 15, 35, Integer.MAX_VALUE};
    private static final int[] FALLBACK_THRESHOLD_VALUES = {3, 6, 10, 15};

    private SwitchCompat killSwitchSwitch;
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
    private SwitchCompat showSpeedInNotificationSwitch;
    private SwitchCompat showTrafficInNotificationSwitch;
    private SwitchCompat showTrafficChartSwitch;
    private SwitchCompat allowLandscapeSwitch;
    private SwitchCompat adBlockSwitch;
    private SwitchCompat domainBlacklistSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.experimental_settings_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        CustomBottomNavigationListener bottomNavigationListener = new CustomBottomNavigationListener(this, R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(bottomNavigationListener);
        bottomNavigationView.setOnItemReselectedListener(bottomNavigationListener);

        killSwitchSwitch = findViewById(R.id.kill_switch_switch);
        killSwitchSwitch.setChecked(SharedPrefUtils.getKillSwitchEnabled(this));
        killSwitchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefUtils.saveKillSwitchEnabled(this, isChecked);
            if (!isChecked && FptnTileService.getServiceStateMutableLiveData().getValue() == ConnectionState.BLOCKED) {
                FptnService.startToDisconnect(this);
            }
        });

        TextView systemKillSwitchLink = findViewById(R.id.kill_switch_system_button);
        systemKillSwitchLink.setPaintFlags(systemKillSwitchLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        systemKillSwitchLink.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_VPN_SETTINGS)));

        customDnsSwitch = findViewById(R.id.custom_dns_switch);
        customDnsInput = findViewById(R.id.custom_dns_input);
        customDnsInput.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        customDnsSwitch.setChecked(SharedPrefUtils.getCustomDnsEnabled(this));
        customDnsInput.setText(SharedPrefUtils.getCustomDnsIpv4(this));
        updateCustomDnsInputVisibility(customDnsSwitch.isChecked());
        customDnsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateCustomDnsInputVisibility(isChecked);
            SharedPrefUtils.saveCustomDnsEnabled(this, isChecked);
        });
        customDnsInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String dnsValue = s.toString().trim();
                if (isValidDnsAddress(dnsValue)) {
                    SharedPrefUtils.saveCustomDnsIpv4(ExperimentalSettingsActivity.this, dnsValue);
                    customDnsInput.setError(null);
                } else {
                    customDnsInput.setError(getString(R.string.custom_dns_invalid));
                }
            }
        });
        showSpeedInNotificationSwitch = findViewById(R.id.show_speed_in_notification_switch);
        showSpeedInNotificationSwitch.setChecked(SharedPrefUtils.getShowSpeedInNotification(this));
        showSpeedInNotificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveShowSpeedInNotification(this, isChecked));

        showTrafficInNotificationSwitch = findViewById(R.id.show_traffic_in_notification_switch);
        showTrafficInNotificationSwitch.setChecked(SharedPrefUtils.getShowTrafficInNotification(this));
        showTrafficInNotificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveShowTrafficInNotification(this, isChecked));

        showTrafficChartSwitch = findViewById(R.id.show_traffic_chart_switch);
        showTrafficChartSwitch.setChecked(SharedPrefUtils.getShowTrafficChart(this));
        showTrafficChartSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveShowTrafficChart(this, isChecked));

        allowLandscapeSwitch = findViewById(R.id.allow_landscape_switch);
        allowLandscapeSwitch.setChecked(SharedPrefUtils.getAllowLandscape(this));
        allowLandscapeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefUtils.saveAllowLandscape(this, isChecked);
            setRequestedOrientation(isChecked
                    ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });

        adBlockSwitch = findViewById(R.id.ad_block_switch);
        adBlockSwitch.setChecked(SharedPrefUtils.getAdBlockEnabled(this));
        adBlockSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveAdBlockEnabled(this, isChecked));

        domainBlacklistSwitch = findViewById(R.id.domain_blacklist_switch);
        domainBlacklistSwitch.setChecked(SharedPrefUtils.getDomainBlacklistEnabled(this));
        domainBlacklistSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveDomainBlacklistEnabled(this, isChecked));

        linkifySubstring(findViewById(R.id.domain_blacklist_label),
                getString(R.string.domain_blacklist_enable_link), this::showDomainBlacklistDialog);

        switchNetworkType = findViewById(R.id.reconnect_on_change_network_type_switch);
        switchNetworkType.setChecked(SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this));
        switchNetworkType.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, isChecked));

        switchIPAddress = findViewById(R.id.reconnect_on_change_ip_address_switch);
        switchIPAddress.setChecked(SharedPrefUtils.getReconnectOnChangeIPEnabled(this));
        switchIPAddress.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, isChecked));

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
                if (fromUser) {
                    SharedPrefUtils.saveReconnectAttemptsCount(ExperimentalSettingsActivity.this, value);
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
                if (fromUser) {
                    SharedPrefUtils.saveDelayBetweenReconnect(ExperimentalSettingsActivity.this, progress + 1);
                }
            }
        });

        int delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(this);
        seekBarDelayBetween.setProgress(0);
        seekBarDelayBetween.setProgress(delayBetweenReconnect - 1);

        // Quick tile request
        View tileButtonLayout = findViewById(R.id.tile_layout);
        TextView buttonRequestTile = findViewById(R.id.quick_settings_tile_button);
        buttonRequestTile.setPaintFlags(buttonRequestTile.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
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
        resetServerAfterDisconnectOnException.setChecked(SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(this));
        resetServerAfterDisconnectOnException.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(this, isChecked));
        updateExceptionVisibility(resetServerAfterDisconnectSwitch.isChecked());

        resetServerAfterDisconnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateExceptionVisibility(isChecked);
            SharedPrefUtils.saveResetSelectedServerEnabled(this, isChecked);
        });

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
                if (fromUser) {
                    SharedPrefUtils.saveAutoFallbackThreshold(ExperimentalSettingsActivity.this, FALLBACK_THRESHOLD_VALUES[progress]);
                }
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
        autoFallbackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAutoFallbackThresholdVisibility(isChecked);
            SharedPrefUtils.saveAutoFallbackEnabled(this, isChecked);
        });

        TextView resetToDefaultLink = findViewById(R.id.reset_to_default_button);
        resetToDefaultLink.setPaintFlags(resetToDefaultLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        resetToDefaultLink.setOnClickListener(v -> resetToDefault());
    }

    private void resetToDefault() {
        killSwitchSwitch.setChecked(false);
        customDnsSwitch.setChecked(false);
        customDnsInput.setText("");
        showSpeedInNotificationSwitch.setChecked(false);
        showTrafficInNotificationSwitch.setChecked(false);
        showTrafficChartSwitch.setChecked(true);
        adBlockSwitch.setChecked(true);
        domainBlacklistSwitch.setChecked(true);
        switchNetworkType.setChecked(true);
        switchIPAddress.setChecked(true);
        seekBarAttemptsCount.setProgress(2); // default 35
        seekBarDelayBetween.setProgress(0); // default 1s
        resetServerAfterDisconnectSwitch.setChecked(true);
        resetServerAfterDisconnectOnException.setChecked(false);
        autoFallbackSwitch.setChecked(true);
        seekBarFallbackThreshold.setProgress(3); // default 15

        SharedPrefUtils.saveKillSwitchEnabled(this, false);
        SharedPrefUtils.saveCustomDnsEnabled(this, false);
        SharedPrefUtils.saveCustomDnsIpv4(this, "");
        SharedPrefUtils.saveShowSpeedInNotification(this, false);
        SharedPrefUtils.saveShowTrafficInNotification(this, false);
        SharedPrefUtils.saveShowTrafficChart(this, true);
        SharedPrefUtils.saveAdBlockEnabled(this, true);
        SharedPrefUtils.saveDomainBlacklistEnabled(this, true);
        SharedPrefUtils.saveDomainBlacklistDomains(this, Constants.DOMAIN_BLACKLIST_DEFAULT);
        SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, true);
        SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, true);
        SharedPrefUtils.saveReconnectAttemptsCount(this, ATTEMPTS_COUNT_VALUES[2]);
        SharedPrefUtils.saveDelayBetweenReconnect(this, 1);
        SharedPrefUtils.saveResetSelectedServerEnabled(this, true);
        SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(this, false);
        SharedPrefUtils.saveAutoFallbackEnabled(this, true);
        SharedPrefUtils.saveAutoFallbackThreshold(this, FALLBACK_THRESHOLD_VALUES[3]);

        XLog.tag(TAG).i("Experimental settings reset to default");
        Toast.makeText(this, R.string.reset_to_default_success, Toast.LENGTH_SHORT).show();
    }

    private void updateCustomDnsInputVisibility(boolean isEnabled) {
        customDnsInput.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    // Turns the given substring of a TextView's text into an inline clickable link, styled like the
    // other links on this screen (white at 70% alpha, underlined). No-op if the substring is absent.
    private void linkifySubstring(TextView textView, String linkText, Runnable onClick) {
        CharSequence fullText = textView.getText();
        int linkStart = fullText.toString().indexOf(linkText);
        if (linkStart < 0) {
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(0xB3FFFFFF); // white at 70% alpha, like other links on this screen
                ds.setUnderlineText(true);
            }
        }, linkStart, linkStart + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showDomainBlacklistDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(6);
        input.setMaxLines(10);
        input.setVerticalScrollBarEnabled(true);
        input.setHint(R.string.domain_blacklist_hint);
        input.setText(SharedPrefUtils.getDomainBlacklistDomains(this));

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.domain_blacklist_title)
                .setView(container)
                .setPositiveButton(R.string.save_button, (dialog, which) ->
                        SharedPrefUtils.saveDomainBlacklistDomains(this, input.getText().toString()))
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateExceptionVisibility(boolean isVisible) {
        resetServerAfterDisconnectOnException.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void updateAutoFallbackThresholdVisibility(boolean isEnabled) {
        autoFallbackThresholdLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestQuickSettingsTile(TextView buttonRequestTile) {
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

    private void handleTileRequestResult(int resultCode, TextView button) {
        // Re-enable button unless the tile was successfully added/exists
        boolean shouldKeepDisabled = false;

        switch (resultCode) {
            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED:
                XLog.tag(TAG).i("Quick settings tile already present");
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
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
