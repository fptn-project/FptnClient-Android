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

package org.fptn.vpn;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fptn.vpn.utils.SharedPrefUtils;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.PatternFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;

import java.io.File;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initXLog();
        registerOrientationController();
    }

    // Portrait is the manifest baseline; on tablets (or when the user opts in) we
    // relax it to free rotation for every activity from a single place.
    private void registerOrientationController() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                applyOrientation(activity);
            }

            // Re-apply on resume so a settings change takes effect on already-created
            // screens (onActivityCreated fires only once, at creation).
            @Override public void onActivityResumed(@NonNull Activity activity) {
                applyOrientation(activity);
            }

            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void applyOrientation(@NonNull Activity activity) {
        final int orientation = SharedPrefUtils.getAllowLandscape(activity)
                ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        try {
            activity.setRequestedOrientation(orientation);
        } catch (IllegalStateException ignored) {
            // API 26 forbids setRequestedOrientation on translucent activities.
        }
    }

    private void initXLog() {
        File logDir = new File(getFilesDir(), "logs2");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String logPath = logDir.getAbsolutePath() + File.separator;

        PatternFlattener flattener = new PatternFlattener("{d yyyy-MM-dd HH:mm:ss}|{l}|{t}|{m}");

        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.INFO)
                .tag("FPTN")
                .build();

        FilePrinter filePrinter = new FilePrinter.Builder(logPath)
                .fileNameGenerator(new DateFileNameGenerator())
                .backupStrategy(new FileSizeBackupStrategy2(512 * 1024, 10))
                .cleanStrategy(new FileLastModifiedCleanStrategy(60 * 60 * 1000L))
                .flattener(flattener)
                .build();
        XLog.init(config, filePrinter, new AndroidPrinter());

        XLog.i("XLog initialized successfully");
    }
}
