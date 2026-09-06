package org.fptn.vpn.ui.common

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.fptn.vpn.R

/**
 * Compose port of `CustomBottomNavigationListener.createShareDialog()`: a QR code for
 * [R.string.play_market_link] plus the HTML info message, "share via message" /
 * "close" actions.
 */
@Composable
fun ShareDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val qrBitmap = remember { generateQrCode(context.getString(R.string.play_market_link), 500, 500) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(300.dp),
                )
                HtmlLinkText(
                    html = stringResource(R.string.info_message_html),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                Text(stringResource(R.string.menu_share))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                shareViaMessage(context)
                onDismiss()
            }) {
                Text(stringResource(R.string.share_via_message))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button_text))
            }
        },
    )
}

private fun shareViaMessage(context: android.content.Context) {
    val shareTitle = context.getString(R.string.share_title)
    val shareMessage = context.getString(R.string.share_message)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareMessage)
    }
    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
}

private fun generateQrCode(text: String, width: Int, height: Int): Bitmap {
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bitmap
}
