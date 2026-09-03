package org.fptn.vpn.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * Port of `ViewUtils.linkifySubstring`: renders [text] as plain text except for the first
 * occurrence of [linkText], which is styled like the legacy `ClickableSpan` (white at 70%
 * alpha, underlined) and invokes [onLinkClick] when tapped.
 */
@Composable
fun LinkifiedText(
    text: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val linkStart = text.indexOf(linkText)
    val linkEnd = linkStart + linkText.length
    val annotated = remember(text, linkText) {
        buildAnnotatedString {
            append(text)
            if (linkStart >= 0) {
                addStyle(
                    SpanStyle(color = Color(0xB3FFFFFFL), textDecoration = TextDecoration.Underline),
                    linkStart,
                    linkEnd,
                )
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = fontSize),
        onClick = { offset ->
            if (linkStart >= 0 && offset in linkStart until linkEnd) {
                onLinkClick()
            }
        },
    )
}
