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
            if (fontSize != TextUnit.Unspecified) textView.textSize = fontSize.value
            textView.setTypeface(textView.typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        },
    )
}
