package org.fptn.vpn.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.fptn.vpn.R;

import lombok.SneakyThrows;

public class CustomBottomNavigationListener implements NavigationBarView.OnItemSelectedListener {
    private final Context context;
    private final int currentViewId;

    public CustomBottomNavigationListener(Context context, BottomNavigationView bottomNavigationView, int currentViewId) {
        this.context = context;
        this.currentViewId = currentViewId;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == currentViewId) {
            return true;
        }
        if (itemId == R.id.menuHome) {
            Intent intent = new Intent(context, HomeActivity.class);
            context.startActivity(intent);

            return true;
        } else if (itemId == R.id.menuSettings) {
            Intent intent = new Intent(context, SettingsActivity.class);
            context.startActivity(intent);

            return true;
        } else if (itemId == R.id.menuShare) {
            createShareDialog();

            return false;
        }
        return false;
    }

    private void createShareDialog() {
        Bitmap qrBitmap = generateQRCode(context.getString(R.string.play_market_link), 500, 500);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.share_dialog, null);

        ImageView qrImageView = dialogView.findViewById(R.id.qr_code_image);
        qrImageView.setImageBitmap(qrBitmap);

        TextView textView = dialogView.findViewById(R.id.link_text);
        textView.setText(Html.fromHtml(context.getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setMessage(R.string.menu_share)
                .setPositiveButton(R.string.share_via_message, (d, which) -> shareViaMessage())
                .setNeutralButton(R.string.close_button_text, null)
                .create();
        dialog.show();
    }

    private void shareViaMessage() {
        final String shareTitle = context.getString(R.string.share_title);
        final String shareMessage = context.getString(R.string.share_message);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        context.startActivity(Intent.createChooser(shareIntent, shareTitle));
    }

    @SneakyThrows
    private Bitmap generateQRCode(String text, int width, int height) {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }
}
