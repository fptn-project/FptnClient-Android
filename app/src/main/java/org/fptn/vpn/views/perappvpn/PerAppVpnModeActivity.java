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

package org.fptn.vpn.views.perappvpn;

import static org.fptn.vpn.utils.ViewUtils.hideView;
import static org.fptn.vpn.utils.ViewUtils.showView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.views.CustomBottomNavigationListener;

public class PerAppVpnModeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private PerAppVpnModeViewModel viewModel;

    private View selectAppsListLayout;
    private RecyclerView appListRecyclerView;
    private EditText searchAppsEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_per_app_vpnmode_layout);

        initializeVariable();
    }

    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(PerAppVpnModeViewModel.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        CustomBottomNavigationListener bottomNavigationListener = new CustomBottomNavigationListener(this, R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(bottomNavigationListener);
        bottomNavigationView.setOnItemReselectedListener(bottomNavigationListener);

        selectAppsListLayout = findViewById(R.id.select_apps_list_layout);

        searchAppsEditText = findViewById(R.id.search_apps_edit_text);
        searchAppsEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter adapter) {
                    adapter.filter(s.toString());
                }
            }
        });

        SwitchCompat showSystemAppsSwitch = findViewById(R.id.show_system_apps_switch);
        showSystemAppsSwitch.setChecked(viewModel.isShowSystemApps());
        showSystemAppsSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                viewModel.setShowSystemApps(isChecked));

        // Setup RadioGroup listener
        RadioGroup protocolRadioGroup = findViewById(R.id.per_app_vpn_mode_radio_button_group);
        protocolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.all_app_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=OFF]");
                viewModel.setPerAppVpnMode(PerAppVpnMode.OFF);
            } else if (checkedId == R.id.allowed_apps_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=ONLY_ALLOWED]");
                viewModel.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED);
            } else if (checkedId == R.id.disallowed_apps_mode_radio_button) {
                XLog.tag(TAG).i("Per-app VPN mode changed [mode=EXCEPT_DISALLOWED]");
                viewModel.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED);
            }
        });

        RadioButton perAppVpnModeOffRadioButton = findViewById(R.id.all_app_mode_radio_button);
        RadioButton onlyAllowedAppsRadioButton = findViewById(R.id.allowed_apps_mode_radio_button);
        RadioButton exceptDisAllowedRadioButton = findViewById(R.id.disallowed_apps_mode_radio_button);
        viewModel.getPerAppVpnModeMutableLiveData().observe(this, perAppVpnMode -> {
            switch (perAppVpnMode) {
                case OFF:
                    perAppVpnModeOffRadioButton.setChecked(true);
                    hideView(selectAppsListLayout);
                    break;
                case ONLY_ALLOWED:
                    onlyAllowedAppsRadioButton.setChecked(true);
                    showView(selectAppsListLayout);

                    if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter appInfoListAdapter) {
                        appInfoListAdapter.setPerAppVpnMode(PerAppVpnMode.ONLY_ALLOWED);
                    }

                    break;
                case EXCEPT_DISALLOWED:
                    exceptDisAllowedRadioButton.setChecked(true);
                    showView(selectAppsListLayout);

                    if (appListRecyclerView.getAdapter() instanceof AppInfoListAdapter appInfoListAdapter) {
                        appInfoListAdapter.setPerAppVpnMode(PerAppVpnMode.EXCEPT_DISALLOWED);
                    }

                    break;
            }
        });

        appListRecyclerView = findViewById(R.id.apps_recycler_view);
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Show a progress bar while loading if you have one in your XML
        ProgressBar progressBar = findViewById(R.id.loading_apps_progress_bar);

        viewModel.getAppListMutableLiveData().observe(this, apps -> {
            AppInfoListAdapter adapter = new AppInfoListAdapter(apps,
                    viewModel.getPerAppVpnModeMutableLiveData().getValue(),
                    viewModel::saveSelectedApps);
            String query = searchAppsEditText.getText().toString();
            if (!query.isEmpty()) adapter.filter(query);
            appListRecyclerView.setAdapter(adapter);

            hideView(progressBar);
        });

        // Trigger the load
        viewModel.loadInstalledApps(getPackageManager());
    }
}
