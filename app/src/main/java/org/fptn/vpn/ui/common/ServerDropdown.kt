package org.fptn.vpn.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity

/**
 * A dropdown of servers rendered with [ServerRow], styled like the legacy `CustomSpinner`.
 * The closed state swaps between `spinner_background_down`/`spinner_background_up` exactly like
 * `CustomSpinner.performClick()`/`performClosedEvent()` did — arrow down at rest, arrow up while
 * the list is showing.
 *
 * The app's [FptnTheme][org.fptn.vpn.ui.theme.FptnTheme] forces a dark Material3 color scheme
 * everywhere, so the popup needs an explicit light [containerColor] — the legacy Spinner's
 * native popup window used the OS's default (light) popup background, never this app's theme.
 * The menu is also explicitly widened to the anchor's own measured width (Material3's default is
 * only as wide as its widest row) and height-capped so a long server list scrolls within the menu
 * — both because a native `Spinner`'s popup always matched the spinner's width, and because an
 * unbounded menu that can't fit below the anchor gets flipped above it by Compose instead.
 */
@Composable
fun ServerDropdown(
    servers: List<ServerEntity>,
    selected: ServerEntity,
    onSelect: (ServerEntity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorWidthPx = it.size.width }
                .legacyDrawableBackground(
                    if (expanded) R.drawable.spinner_background_up else R.drawable.spinner_background_down,
                )
                .clickable(enabled = enabled) { expanded = true },
        ) {
            ServerRow(server = selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(density) { anchorWidthPx.toDp() })
                .heightIn(max = 220.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = { ServerRow(server = server) },
                    onClick = {
                        onSelect(server)
                        expanded = false
                    },
                )
            }
        }
    }
}
