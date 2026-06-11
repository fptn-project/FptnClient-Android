package org.fptn.vpn.ui.components

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10

enum class BottomNavTab { HOME, SETTINGS, SHARE }

@Composable
fun BottomNavBar(
    current: BottomNavTab,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    context: Context,
) {
    NavigationBar(
        containerColor = Primary,
        contentColor = White,
    ) {
        NavigationBarItem(
            selected = current == BottomNavTab.HOME,
            onClick = { if (current != BottomNavTab.HOME) onHome() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_home_24),
                    contentDescription = stringResource(R.string.menu_home)
                )
            },
            label = { Text(stringResource(R.string.menu_home)) },
            colors = navItemColors(),
        )
        NavigationBarItem(
            selected = current == BottomNavTab.SETTINGS,
            onClick = { if (current != BottomNavTab.SETTINGS) onSettings() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_settings_24),
                    contentDescription = stringResource(R.string.menu_settings)
                )
            },
            label = { Text(stringResource(R.string.menu_settings)) },
            colors = navItemColors(),
        )
        NavigationBarItem(
            selected = false,
            onClick = { showShareDialog(context) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_share_24),
                    contentDescription = stringResource(R.string.menu_share)
                )
            },
            label = { Text(stringResource(R.string.menu_share)) },
            colors = navItemColors(),
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Secondary,
    selectedTextColor = Secondary,
    unselectedIconColor = White10,
    unselectedTextColor = White10,
    indicatorColor = Color.Transparent,
)

private fun showShareDialog(context: Context) {
    val qrBitmap = generateQRCode(context.getString(R.string.play_market_link), 500, 500)
    val inflater = android.view.LayoutInflater.from(context)
    val dialogView = inflater.inflate(R.layout.share_dialog, null)

    (dialogView.findViewById<ImageView>(R.id.qr_code_image)).setImageBitmap(qrBitmap)
    val textView = dialogView.findViewById<TextView>(R.id.link_text)
    textView.text = Html.fromHtml(context.getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY)
    textView.movementMethod = LinkMovementMethod.getInstance()

    AlertDialog.Builder(context)
        .setView(dialogView)
        .setMessage(R.string.menu_share)
        .setPositiveButton(R.string.share_via_message) { _, _ -> shareViaMessage(context) }
        .setNeutralButton(R.string.close_button_text, null)
        .create()
        .show()
}

private fun shareViaMessage(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message))
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_title)))
}

private fun generateQRCode(text: String, width: Int, height: Int): Bitmap {
    val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bitmap
}
