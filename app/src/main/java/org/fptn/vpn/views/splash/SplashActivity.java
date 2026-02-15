package org.fptn.vpn.views.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.database.AppDatabase;
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
                    intent = new Intent(SplashActivity.this, HomeActivity.class);
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
        }, getMainExecutor());
    }

}
