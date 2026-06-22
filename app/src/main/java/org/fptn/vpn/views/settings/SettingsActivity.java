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

package org.fptn.vpn.views.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.utils.PermissionsUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.experimentalsettings.ExperimentalSettingsActivity;
import org.fptn.vpn.views.log.LogsActivity;
import org.fptn.vpn.views.splash.SplashActivity;
import org.fptn.vpn.views.adapter.ServerEntityAdapter;
import org.fptn.vpn.views.bypassmethod.BypassMethodsActivity;
import org.fptn.vpn.views.perappvpn.PerAppVpnModeActivity;
import org.fptn.vpn.views.updatetoken.UpdateTokenActivity;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private ListView serverListView;

    private SettingsViewModel viewModel;

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
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));


        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        viewModel.loadServersList();
        viewModel.getServerDtoListLiveData().observe(this, serverEntities -> {
            if (serverEntities != null && !serverEntities.isEmpty()) {
                serverListView.setAdapter(new ServerEntityAdapter(serverEntities, R.layout.settings_server_list_item));
                setListViewHeightBasedOnChildren(serverListView);
            }
        });
        serverListView = findViewById(R.id.settings_servers_list);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            final String version = pInfo.versionName;

            TextView versionTextView = findViewById(R.id.settings_fptn_version);
            versionTextView.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            XLog.tag(TAG).e("Failed to read app version: %s", e.getMessage());
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
        experimentalFeaturesLayout.setOnClickListener(this::onExperimentalSettings);

        View logoutLayout = findViewById(R.id.logout_layout);
        logoutLayout.setOnClickListener(this::onLogout);

        // NEW: Bypass methods layout click listener
        View bypassMethodsLayout = findViewById(R.id.bypass_methods_layout);
        bypassMethodsLayout.setOnClickListener(this::onBypassMethods);

        // NEW: Per-app VPN mode layout click listener
        View perAppVPNLayout = findViewById(R.id.per_app_vpn_mode_layout);
        perAppVPNLayout.setOnClickListener(this::perAppVpnMode);

        // Logs viewer
        View logsViewerLayout = findViewById(R.id.logs_viewer_layout);
        if (logsViewerLayout != null) {
            logsViewerLayout.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, LogsActivity.class);
                startActivity(intent);
            });
        }
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
                    XLog.tag(TAG).w("Battery optimization exemption denied by user");
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
                    XLog.tag(TAG).w("Background data transfer permission denied by user");
                    permissionBackgroundDataTransferButton.setChecked(false);
                })
                .show();
    }

    private void onExperimentalSettings(View view) {
        Intent intent = new Intent(SettingsActivity.this, ExperimentalSettingsActivity.class);
        startActivity(intent);
    }

    public void onLogout(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    viewModel.deleteAllServers();
                    // goto Login activity
                    Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void onUpdateToken(View v) {
        // Goto update token
        Intent intent = new Intent(SettingsActivity.this, UpdateTokenActivity.class);
        startActivity(intent);
    }

    public void onBypassMethods(View v) {
        Intent intent = new Intent(SettingsActivity.this, BypassMethodsActivity.class);
        startActivity(intent);
    }

    private void perAppVpnMode(View view) {
        Intent intent = new Intent(SettingsActivity.this, PerAppVpnModeActivity.class);
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
}
