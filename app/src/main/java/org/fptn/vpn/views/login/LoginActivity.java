package org.fptn.vpn.views.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.views.home.HomeActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private LoginActivityViewModel viewModel;
    private EditText linkEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        initializeVariable();
    }

    @SuppressLint({"InlinedApi", "ClickableViewAccessibility"})
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);

        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.telegram_bot_html), Html.FROM_HTML_MODE_LEGACY));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        TextView errorTextView = findViewById(R.id.errorTextView);
        viewModel.getErrorTextLiveData().observe(this, errorTextView::setText);

        linkEditText = findViewById(R.id.fptn_login_link_input);

        Button loginButton = findViewById(R.id.fptn_login_button);
        loginButton.setOnClickListener((v) -> onLogin());

        // hide keyboard
        linkEditText.setTextIsSelectable(true);
        linkEditText.setShowSoftInputOnFocus(false);
        linkEditText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (motionEvent.getX() > (view.getWidth() - view.getPaddingRight() - 50)) {
                    ((EditText) view).setText("");
                }
            }
            return false;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts

        // miss back button
        getOnBackPressedDispatcher().addCallback(this, new MyOnBackPressedCallback());
    }

    public void onLogin() {
        final String tokenLink = linkEditText.getText().toString();
        try {
            ListenableFuture<Void> updateResult = viewModel.parseAndSaveToken(tokenLink);
            Futures.addCallback(updateResult, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);

                    finish();
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Error occurs: " + t.getMessage(), t);
                    Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    viewModel.getErrorTextLiveData().postValue(t.getMessage());
                }
            }, getMainExecutor());
        } catch (Exception e) {
            Log.e(TAG, "Token invalid: ", e);
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();
            viewModel.getErrorTextLiveData().postValue(getString(R.string.token_saving_failed));
        }
    }

    private static class MyOnBackPressedCallback extends OnBackPressedCallback {
        public MyOnBackPressedCallback() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {

        }
    }
}
