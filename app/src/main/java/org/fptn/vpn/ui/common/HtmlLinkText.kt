package org.fptn.vpn.ui.common

import android.graphics.Typeface
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders an HTML string (as produced by `Html.fromHtml(..., FROM_HTML_MODE_LEGACY)`) with
 * clickable `<a>` links, exactly like the legacy screens did with a plain `TextView` +
 * `LinkMovementMethod`. Implemented as a thin `AndroidView` wrapper rather than an
 * `AnnotatedString` reimplementation, so link styling and tap targets stay byte-for-byte
 * identical to the pre-Compose behavior.
 */
@Composable
fun HtmlLinkText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    padding: PaddingValues = PaddingValues(0.dp),
    bold: Boolean = false,
) {
    val density = LocalDensity.current
    val paddingLeft = with(density) { padding.calculateLeftPadding(LayoutDirection.Ltr).roundToPx() }
    val paddingTop = with(density) { padding.calculateTopPadding().roundToPx() }
    val paddingRight = with(density) { padding.calculateRightPadding(LayoutDirection.Ltr).roundToPx() }
    val paddingBottom = with(density) { padding.calculateBottomPadding().roundToPx() }

    // If fontSize is not provided, use 14.sp (the Compose/Material default for body text).
    // This is necessary because the native TextView reads fontScale from the system configuration,
    // while we override fontScale only in Compose through LocalDensity.
    // Without this, TextView will ignore our custom fontScale override.
    val effectiveFontSize = if (fontSize != TextUnit.Unspecified) fontSize else 14.sp

    // Convert to pixels using LocalDensity — this accounts for both screen density and our fontScale.
    val fontSizePx = with(density) { effectiveFontSize.toPx() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            if (color != Color.Unspecified) textView.setTextColor(color.toArgb())
            // Always set the text size to ensure that the fontScale
            // from LocalDensity is applied, even if the caller does not provide fontSize.
            textView.setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                fontSizePx,
            )
            textView.setTypeface(textView.typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        },
    )
}
