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

package org.fptn.vpn.views.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.services.snichecker.SniCheckerService;
import org.fptn.vpn.services.snichecker.SniCheckerServiceState;
import org.fptn.vpn.views.bypassmethod.BypassMethodsActivity;
import org.fptn.vpn.views.home.HomeActivity;
import org.fptn.vpn.views.login.LoginActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        initializeVariable();
    }

    private void initializeVariable() {
        ListenableFuture<Integer> countFuture = AppDatabase.getInstance(this).serverDAO().getCount();
        Futures.addCallback(countFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Integer count) {
                Intent intent;
                if (count > 0) {
                    if (SniCheckerService.getStaticServiceState().getValue() == SniCheckerServiceState.ACTIVE) {
                        intent = new Intent(SplashActivity.this, BypassMethodsActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, HomeActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                // Default to Login on error
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

}
