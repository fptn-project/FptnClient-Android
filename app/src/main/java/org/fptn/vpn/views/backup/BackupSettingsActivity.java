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

package org.fptn.vpn.views.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.utils.backup.SettingsBackupManager;
import org.fptn.vpn.views.CustomBottomNavigationListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

public class BackupSettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<Intent> openFileLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_backup_layout);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        CustomBottomNavigationListener bottomNavigationListener = new CustomBottomNavigationListener(this, R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(bottomNavigationListener);
        bottomNavigationView.setOnItemReselectedListener(bottomNavigationListener);

        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null && result.getData().getData() != null) {
                        writeBackupToUri(result.getData().getData());
                    }
                });

        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null && result.getData().getData() != null) {
                        confirmAndRestore(result.getData().getData());
                    }
                });

        findViewById(R.id.backup_save_button).setOnClickListener(v -> onSaveToFile());
        findViewById(R.id.backup_share_button).setOnClickListener(v -> onShare());
        findViewById(R.id.backup_restore_button).setOnClickListener(v -> onLoadFromFile());
    }

    private void onSaveToFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, generateBackupFileName());
        createFileLauncher.launch(intent);
    }

    private void onLoadFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Downloaded/shared *.json files often come with a generic mime type
        intent.setType("*/*");
        openFileLauncher.launch(intent);
    }

    private void writeBackupToUri(Uri uri) {
        new Thread(() -> {
            try {
                String json = SettingsBackupManager.exportToJson(this);
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri, "wt")) {
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                }
                XLog.tag(TAG).i("Settings backup saved [uri=%s]", uri);
                runOnUiThread(() -> Toast.makeText(this, R.string.backup_saved_success, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                XLog.tag(TAG).e("Failed to save settings backup: %s", e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, R.string.backup_save_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void onShare() {
        new Thread(() -> {
            try {
                String json = SettingsBackupManager.exportToJson(this);

                File shareDir = new File(getCacheDir(), "backups");
                shareDir.mkdirs();
                File backupFile = new File(shareDir, generateBackupFileName());
                try (OutputStream outputStream = new FileOutputStream(backupFile)) {
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                }

                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", backupFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                XLog.tag(TAG).i("Sharing settings backup [file=%s]", backupFile.getName());
                runOnUiThread(() -> startActivity(
                        Intent.createChooser(shareIntent, getString(R.string.backup_share_button))));
            } catch (Exception e) {
                XLog.tag(TAG).e("Failed to share settings backup: %s", e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, R.string.backup_save_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmAndRestore(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_confirm_title)
                .setMessage(R.string.backup_restore_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    restoreFromUri(uri);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void restoreFromUri(Uri uri) {
        new Thread(() -> {
            try {
                String json;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                    json = reader.lines().collect(Collectors.joining("\n"));
                }
                SettingsBackupManager.importFromJson(this, json);
                XLog.tag(TAG).i("Settings restored from backup [uri=%s]", uri);
                runOnUiThread(() -> Toast.makeText(this, R.string.backup_restore_success, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                XLog.tag(TAG).e("Failed to restore settings backup: %s", e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, R.string.backup_restore_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static String generateBackupFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        return "fptn-settings-" + timestamp + ".json";
    }
}
