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

package org.fptn.vpn.views.trustedwifi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.services.vpn.FptnService;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.views.CustomBottomNavigationListener;

import java.util.Set;

public class TrustedWifiActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private SwitchCompat switchTrustedWifi;
    private TextView tvCurrentWifiSsid;
    private Button btnAddCurrentWifi;
    private LinearLayout trustedWifiListContainer;
    private TextView tvEmptyTrustedWifi;
    private BottomNavigationView bottomNavigationView;

    private String currentSsid = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_wifi);

        initViews();
        checkPermissionsAndRefresh();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        switchTrustedWifi = findViewById(R.id.switch_trusted_wifi);
        tvCurrentWifiSsid = findViewById(R.id.tv_current_wifi_ssid);
        btnAddCurrentWifi = findViewById(R.id.btn_add_current_wifi);
        trustedWifiListContainer = findViewById(R.id.trusted_wifi_list_container);
        tvEmptyTrustedWifi = findViewById(R.id.tv_empty_trusted_wifi);

        boolean enabled = SharedPrefUtils.getTrustedWifiEnabled(this);
        switchTrustedWifi.setChecked(enabled);
        switchTrustedWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefUtils.saveTrustedWifiEnabled(this, isChecked);
            if (isChecked) {
                checkPermissionsAndRefresh();
            }
        });

        btnAddCurrentWifi.setOnClickListener(v -> {
            if (currentSsid != null && !currentSsid.isEmpty()) {
                SharedPrefUtils.addTrustedWifiSsid(this, currentSsid);
                Toast.makeText(this, getString(R.string.trusted_wifi_title) + ": " + currentSsid, Toast.LENGTH_SHORT).show();
                refreshUI();
            }
        });
    }

    private void checkPermissionsAndRefresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        refreshUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            refreshUI();
        }
    }

    private void refreshUI() {
        currentSsid = FptnService.getCurrentWifiSsid(this);

        if (currentSsid != null && !currentSsid.isEmpty()) {
            tvCurrentWifiSsid.setText(currentSsid);
            boolean isAlreadyTrusted = SharedPrefUtils.isWifiSsidTrusted(this, currentSsid);
            if (isAlreadyTrusted) {
                btnAddCurrentWifi.setText(String.format(getString(R.string.trusted_wifi_already_added), currentSsid));
                btnAddCurrentWifi.setEnabled(false);
                btnAddCurrentWifi.setVisibility(View.VISIBLE);
            } else {
                btnAddCurrentWifi.setText(String.format(getString(R.string.trusted_wifi_add_current), currentSsid));
                btnAddCurrentWifi.setEnabled(true);
                btnAddCurrentWifi.setVisibility(View.VISIBLE);
            }
        } else {
            tvCurrentWifiSsid.setText(getString(R.string.trusted_wifi_not_connected));
            btnAddCurrentWifi.setVisibility(View.GONE);
        }

        // Render saved SSIDs list
        Set<String> ssids = SharedPrefUtils.getTrustedWifiSsids(this);
        trustedWifiListContainer.removeAllViews();

        if (ssids == null || ssids.isEmpty()) {
            trustedWifiListContainer.addView(tvEmptyTrustedWifi);
        } else {
            for (String ssid : ssids) {
                View itemView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, trustedWifiListContainer, false);
                TextView textView = itemView.findViewById(android.R.id.text1);
                textView.setText("📶 " + ssid + "  ✕");
                textView.setTextColor(ContextCompat.getColor(this, R.color.white));
                textView.setPadding(24, 24, 24, 24);

                itemView.setOnClickListener(v -> {
                    SharedPrefUtils.removeTrustedWifiSsid(this, ssid);
                    refreshUI();
                });

                trustedWifiListContainer.addView(itemView);
            }
        }
    }
}
