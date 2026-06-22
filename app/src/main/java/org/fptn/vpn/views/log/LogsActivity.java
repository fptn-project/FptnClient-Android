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

package org.fptn.vpn.views.log;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fptn.vpn.R;
import org.fptn.vpn.views.CustomBottomNavigationListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

public class LogsActivity extends AppCompatActivity {

    private TextView tvLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logs_layout);

        tvLogs = findViewById(R.id.tv_logs);
        tvLogs.setText(loadLogs());
        tvLogs.setOnClickListener(v -> copyLogs());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(
                new CustomBottomNavigationListener(this, R.id.menuSettings));
    }

    private void copyLogs() {
        String text = tvLogs.getText().toString();
        if (text == null || text.isEmpty()) {
            Toast.makeText(this, R.string.logs_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("logs", text));
        Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show();
    }

    private String loadLogs() {
        File logDir = new File(getFilesDir(), "logs2");
        if (!logDir.exists() || !logDir.isDirectory()) {
            return "No logs directory.";
        }
        File[] files = logDir.listFiles();
        if (files == null || files.length == 0) {
            return "No log files.";
        }
        File latest = Arrays.stream(files)
                .filter(f -> f.isFile() && f.canRead())
                .max((a, b) -> Long.compare(a.lastModified(), b.lastModified()))
                .orElse(null);
        if (latest == null) {
            return "No readable log file.";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(latest))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            return "Error reading log: " + e.getMessage();
        }
        return sb.length() == 0 ? "Log file is empty." : sb.toString();
    }
}

//package org.fptn.vpn.views.log;
//
//import android.content.ClipData;
//import android.content.ClipboardManager;
//import android.content.Context;
//import android.os.Bundle;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//
//import org.fptn.vpn.R;
//import org.fptn.vpn.views.CustomBottomNavigationListener;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.util.Arrays;
//
//public class LogsActivity extends AppCompatActivity {
//
//    private TextView tvLogs;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.logs_layout);
//
//        tvLogs = findViewById(R.id.tv_logs);
//
//        String logs = loadLogs();
//
//        tvLogs.setText(logs);
//
//        tvLogs.setOnLongClickListener(v -> {
//            copyLogs();
//            return true;
//        });
//
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
//        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
//        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));
//    }
//
//    public String loadLogs() {
//        File logDir = new File(getFilesDir(), "logs2");
//
//        if (!logDir.exists()) {
//            return "Log directory does not exist:\n" + logDir.getAbsolutePath() +
//                    "\n\nPlease run the app and trigger some XLog.i() calls.";
//        }
//        if (!logDir.isDirectory()) {
//            return "Path exists but is not a directory:\n" + logDir.getAbsolutePath();
//        }
//
//        File[] files = logDir.listFiles();
//        if (files == null || files.length == 0) {
//            return "No files in:\n" + logDir.getAbsolutePath() +
//                    "\n\nCheck if XLog was initialized and write permission is OK.";
//        }
//
//        StringBuilder report = new StringBuilder();
//        report.append("Directory: ").append(logDir.getAbsolutePath()).append("\n");
//        report.append("Files count: ").append(files.length).append("\n");
//        for (File f : files) {
//            report.append("• ").append(f.getName())
//                    .append(" (").append(f.length()).append(" bytes")
//                    .append(f.canRead() ? ", readable" : ", NOT readable")
//                    .append(")\n");
//        }
//
//        File latestFile = null;
//        long latestTime = 0;
//        for (File f : files) {
//            if (f.isFile() && f.canRead()) {
//                long mod = f.lastModified();
//                if (mod > latestTime) {
//                    latestTime = mod;
//                    latestFile = f;
//                }
//            }
//        }
//
//        if (latestFile == null) {
//            return report.toString() + "\nNo readable log files found.";
//        }
//
//        StringBuilder content = new StringBuilder();
//        content.append(report).append("File: ").append(latestFile.getName()).append("\n");
//        content.append("Last modified: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(latestTime))).append("\n");
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(latestFile))) {
//            String line;
//            int lineCount = 0;
//            while ((line = reader.readLine()) != null) {
//                content.append(line).append("\n");
//                lineCount++;
//            }
//            if (lineCount == 0) {
//                content.append("[File is empty]\n");
//            }
//        } catch (Exception e) {
//            content.append("Error reading file: ").append(e.getMessage()).append("\n");
//        }
//
//        return content.toString();
//    }
//
//    private void copyLogs() {
//        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//        ClipData clip = ClipData.newPlainText("app_logs", tvLogs.getText().toString());
//        clipboard.setPrimaryClip(clip);
//        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
//    }
//}
