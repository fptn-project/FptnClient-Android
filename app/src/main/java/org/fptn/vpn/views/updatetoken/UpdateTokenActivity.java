package org.fptn.vpn.views.updatetoken;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.views.CustomBottomNavigationListener;
import org.fptn.vpn.views.settings.SettingsActivity;

import com.elvishew.xlog.XLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import lombok.Getter;

public class UpdateTokenActivity extends AppCompatActivity {
    private static final String TAG = UpdateTokenActivity.class.getSimpleName();

    @Getter
    private UpdateTokenViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout_update_token);

        initializeVariable();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeVariable() {
        viewModel = new ViewModelProvider(this).get(UpdateTokenViewModel.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, R.id.menuSettings));

        TextView errorTextView = findViewById(R.id.errorTextView);
        viewModel.getErrorTextLiveData().observe(this, errorTextView::setText);

        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.telegram_bot_html), Html.FROM_HTML_MODE_LEGACY));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        // HIDE KEYBOARD
        EditText editText = findViewById(R.id.fptn_login_link_input);
        editText.setTextIsSelectable(true);
        editText.setShowSoftInputOnFocus(false);
        editText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (motionEvent.getX() > (view.getWidth() - view.getPaddingRight() - 50)) {
                    ((EditText) view).setText("");
                }
            }
            return false;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts
    }

    public void onCancel(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        finish();
    }

    public void onSave(View v) {
        EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String tokenLink = linkInput.getText().toString();

        try {
            ListenableFuture<Void> updateResult = viewModel.parseAndSaveToken(tokenLink);
            Futures.addCallback(updateResult, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getApplicationContext(), R.string.token_was_updated, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(UpdateTokenActivity.this, SettingsActivity.class);
                    startActivity(intent);

                    finish();
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Error occurs: " + t.getMessage(), t);
                    Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();

                    viewModel.getErrorTextLiveData().postValue(t.getMessage());
                }

            }, getMainExecutor());
        } catch (Exception e) {
            XLog.tag(TAG).e("Token invalid: ", e);
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();

            viewModel.getErrorTextLiveData().postValue(getString(R.string.token_saving_failed));
        }
    }

}
