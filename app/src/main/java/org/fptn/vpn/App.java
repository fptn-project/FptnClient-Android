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
import org.fptn.vpn.utils.XLogInitializer;

import com.elvishew.xlog.XLog;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initXLog();
        registerOrientationController();
        // Load the font size state before MainActivity starts composition.
        // FontScaleState is a Compose state singleton; reading it in MainActivity is reactive.
        org.fptn.vpn.ui.theme.FontScaleState.INSTANCE.load(this);
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
        XLogInitializer.init(this);
        XLog.i("XLog initialized successfully");
    }
}
