package org.fptn.vpn.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The pill-shaped, all-caps-off text button used across the legacy screens
 * (`round_back_secondary_100` / `round_back_secondary_cancel_100`), reproduced as a plain
 * clickable+background composable rather than a Material `Button`, so the exact shape and
 * padding of the original XML background drawable is preserved instead of Material's own
 * shape/elevation defaults.
 */
@Composable
fun LegacyPillButton(
    text: String,
    @DrawableRes backgroundDrawable: Int,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    contentPadding: Dp = 8.dp,
) {
    BasicText(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .legacyDrawableBackground(backgroundDrawable)
            .padding(contentPadding),
        style = TextStyle(
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        ),
    )
}
