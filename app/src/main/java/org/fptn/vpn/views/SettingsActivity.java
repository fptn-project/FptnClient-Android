package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.services.tile.FptnTileService;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.views.adapter.FptnServerAdapter;

import lombok.Getter;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private ListView serverListView;

    @Getter
    private FptnServerViewModel fptnViewModel;

    private SwitchCompat permissionBatteryOptimizationButton;
    private SwitchCompat permissionBackgroundDataTransferButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuSettings));

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getServerDtoListLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                serverListView.setAdapter(new FptnServerAdapter(fptnServerDtos, R.layout.settings_server_list_item));
                setListViewHeightBasedOnChildren(serverListView);
            } else {
                // goto Login activity
                Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
        serverListView = findViewById(R.id.settings_servers_list);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            final String version = pInfo.versionName;

            TextView versionTextView = findViewById(R.id.settings_fptn_version);
            versionTextView.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't show app version! ", e);
        }

        // about
        TextView about = findViewById(R.id.settings_about);
        about.setText(Html.fromHtml(getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        // token's info
        TextView tokenInfo = findViewById(R.id.settings_token_info_html);
        tokenInfo.setText(Html.fromHtml(getString(R.string.settings_token_info_html), Html.FROM_HTML_MODE_LEGACY));
        tokenInfo.setMovementMethod(LinkMovementMethod.getInstance());

        // Permission settings
        permissionBatteryOptimizationButton = findViewById(R.id.permission_battery_optimization_button);
        permissionBatteryOptimizationButton.setOnClickListener(view -> requestBatteryOptimisationPermission());

        permissionBackgroundDataTransferButton = findViewById(R.id.permission_background_data_transfer_button);
        permissionBackgroundDataTransferButton.setOnClickListener(view -> requestBackgroundDataTransferPermission());

        // Our sponsors
        TextView textView = findViewById(R.id.sponsors_list);
        textView.setText(Html.fromHtml(getString(R.string.sponsors_usernames)));

        // Set on click listeners
        View updateTokenLayout = findViewById(R.id.update_token_layout);
        updateTokenLayout.setOnClickListener(this::onUpdateToken);

        View experimentalFeaturesLayout = findViewById(R.id.experimental_features_layout);
        experimentalFeaturesLayout.setOnClickListener(this::showExperimentalSettingsDialog);

        View logoutLayout = findViewById(R.id.logout_layout);
        logoutLayout.setOnClickListener(this::onLogout);

        // NEW: Bypass methods layout click listener
        View bypassMethodsLayout = findViewById(R.id.bypass_methods_layout);
        bypassMethodsLayout.setOnClickListener(this::onBypassMethods);
    }

    @Override
    protected void onResume() {
        super.onResume();

        bottomNavigationView.setSelectedItemId(R.id.menuSettings);

        setPermissionButtonState(PermissionsUtils.checkBatteryOptimizations(this), permissionBatteryOptimizationButton);
        setPermissionButtonState(PermissionsUtils.checkBackgroundDataTransferRestrictions(this), permissionBackgroundDataTransferButton);
    }

    private void setPermissionButtonState(boolean isGranted, SwitchCompat switchView) {
        switchView.setEnabled(true);
        if (isGranted) {
            switchView.setClickable(false);
            switchView.setChecked(true);
        } else {
            switchView.setClickable(true);
            switchView.setChecked(false);
        }
    }

    private void requestBatteryOptimisationPermission() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.battery_optimization_request_dialog_title))
                .setMessage(getString(R.string.battery_optimization_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Battery optimisation permission denied!");
                    permissionBatteryOptimizationButton.setChecked(false);
                })
                .show();
    }

    private void requestBackgroundDataTransferPermission() {
        /* If somebody worry about low speed in background - disable restriction on network transfer data*/
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.background_data_request_dialog_title))
                .setMessage(getString(R.string.background_data_request_dialog_text))
                .setPositiveButton(getString(R.string.grant), (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.deny), (dialog, which) -> {
                    Log.i(TAG, "Background data transfer permission denied!");
                    permissionBackgroundDataTransferButton.setChecked(false);
                })
                .show();
    }

    public void onLogout(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    fptnViewModel.deleteAll();
                    // goto Login activity
                    Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void onUpdateToken(View v) {
        // Goto update token
        Intent intent = new Intent(SettingsActivity.this, SettingsActivityUpdateToken.class);
        startActivity(intent);
    }

    // NEW: Bypass methods click handler
    public void onBypassMethods(View v) {
        Intent intent = new Intent(SettingsActivity.this, BypassMethodsActivity.class);
        startActivity(intent);
    }

    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public void showExperimentalSettingsDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.experimental_features_label);
        builder.setIcon(R.drawable.ic_experimental_features_24);

        View dialogView = getLayoutInflater().inflate(R.layout.experimental_settings_dialog, null);
        builder.setView(dialogView);

        /* Reconnect on change network type */
        SwitchCompat switchNetworkType = dialogView.findViewById(R.id.reconnect_on_change_network_type_switch);
        switchNetworkType.setChecked(SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(this));

        /* Reconnect on change IP address */
        SwitchCompat switchIPAddress = dialogView.findViewById(R.id.reconnect_on_change_ip_address_switch);
        switchIPAddress.setChecked(SharedPrefUtils.getReconnectOnChangeIPEnabled(this));

        /* Quick tile request */
        Button buttonRequestTile = dialogView.findViewById(R.id.quick_settings_tile_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buttonRequestTile.setOnClickListener(l -> {
                @SuppressLint("WrongConstant") StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                try {
                    // Request to add a custom tile service
                    statusBarManager.requestAddTileService(
                            new ComponentName(this, FptnTileService.class),
                            "FPTN",
                            Icon.createWithResource(this, R.drawable.ic_logo),
                            this.getMainExecutor(),
                            (resultCode) -> {
                                if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Log.d(TAG, "Tile already added successfully. Nothing to do.");
                                    Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT)
                                            .show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Log.d(TAG, "Tile added successfully.");
                                    Toast.makeText(this, R.string.tile_added_successfully, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Log.d(TAG, "User cancel request.");
                                }
                            }
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Failed to request tile addition", e);
                    Toast.makeText(this, R.string.tile_addition_failed, Toast.LENGTH_SHORT)
                            .show();
                }
            });
            buttonRequestTile.setEnabled(true);
            buttonRequestTile.setVisibility(View.VISIBLE);
        } else {
            buttonRequestTile.setEnabled(false);
            buttonRequestTile.setVisibility(View.INVISIBLE);
        }

        /* Reconnects attempts count */
        SeekBar seekBarAttemptsCount = dialogView.findViewById(R.id.seekBarAttemptsCount);
        TextView textViewAttemptsCount = dialogView.findViewById(R.id.textViewAttemptsCount);
        seekBarAttemptsCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 3) {
                    textViewAttemptsCount.setText("∞");
                } else {
                    String format = getString(R.string.reconnect_attempts_text);
                    textViewAttemptsCount.setText(String.format(format, progress * 5));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarAttemptsCount.setProgress(0);
        int reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(this);
        if (reconnectAttemptsCount == Integer.MAX_VALUE) {
            seekBarAttemptsCount.setProgress(3);
        } else {
            seekBarAttemptsCount.setProgress(reconnectAttemptsCount / 5);
        }

        /* Reconnects delay between in seconds */
        SeekBar seekBarDelayBetween = dialogView.findViewById(R.id.seekBarDelayBetween);
        TextView textViewDelayBetween = dialogView.findViewById(R.id.textViewDelayBetween);
        seekBarDelayBetween.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String format = getString(R.string.delay_between_attempts_seconds);
                textViewDelayBetween.setText(String.format(format, progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(this);
        seekBarDelayBetween.setProgress(0);
        seekBarDelayBetween.setProgress(delayBetweenReconnect - 1);

        /* Reset selected server on disconnect */
        SwitchCompat resetServerAfterDisconnectSwitch = dialogView.findViewById(R.id.reset_selected_server_after_disconnect_switch);
        resetServerAfterDisconnectSwitch.setChecked(SharedPrefUtils.getResetSelectedServerEnabled(this));

        builder.setPositiveButton(getString(R.string.save_button), (dialog, which) -> {
            Log.d(TAG, "experimentalFeaturesDialog: save");
            SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(this, switchNetworkType.isChecked());
            SharedPrefUtils.saveReconnectOnChangeIPEnabled(this, switchIPAddress.isChecked());
            SharedPrefUtils.saveResetSelectedServerEnabled(this, resetServerAfterDisconnectSwitch.isChecked());

            int attemptsCountProgress = seekBarAttemptsCount.getProgress();
            if (attemptsCountProgress == 3) {
                SharedPrefUtils.saveReconnectAttemptsCount(this, Integer.MAX_VALUE);
            } else {
                SharedPrefUtils.saveReconnectAttemptsCount(this, attemptsCountProgress * 5);
            }

            int delayBetweenProgress = seekBarDelayBetween.getProgress();
            SharedPrefUtils.saveDelayBetweenReconnect(this, delayBetweenProgress + 1);
        });
        builder.setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {
            Log.d(TAG, "experimentalFeaturesDialog: cancel");
            dialog.dismiss();
        });

        builder.create().show();
    }
}
