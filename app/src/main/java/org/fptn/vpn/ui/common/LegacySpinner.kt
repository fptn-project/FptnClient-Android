package org.fptn.vpn.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.Primary

private val DROPDOWN_ITEM_HEIGHT = 48.dp
private val DROPDOWN_VERTICAL_PADDING = 16.dp

@Composable
fun <T> LegacySpinner(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp? = null,
    maxVisibleItems: Int = 5,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Force LTR for the whole component to prevent popup positioning issues in RTL languages
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = if (width != null) modifier.width(width) else modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .alpha(if (enabled) 1f else 0.4f)
                    .onGloballyPositioned { anchorWidthPx = it.size.width }
                    .legacyDrawableBackground(
                        if (expanded) R.drawable.spinner_background_up else R.drawable.spinner_background_down,
                    )
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label(selected),
                    color = Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { anchorWidthPx.toDp() })
                    .heightIn(max = DROPDOWN_ITEM_HEIGHT * maxVisibleItems + DROPDOWN_VERTICAL_PADDING),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
            ) {
                CompositionLocalProvider(
                    LocalDensity provides density,
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(label(item), color = Color.Black) },
                            onClick = {
                                onSelect(item)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}