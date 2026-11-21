package org.fptn.vpn.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.utils.CountryFlags

/**
 * Compose port of `ServerEntityAdapter`'s row (`home_list_recycler_server_item.xml`): a real
 * server gets a flag emoji, name, an optional censored icon, and (once pinged) a ping line below
 * (colored emoji + "Nms", "---  ---  ---" for unreachable) — two lines tall. The "Auto"
 * pseudo-server has no second line, so its logo and label are enlarged instead, making its single
 * line match a pinged real server's total two-line height rather than leaving dead space below.
 */
@Composable
fun ServerRow(server: ServerEntity, pingValue: Long, modifier: Modifier = Modifier) {
    // Force LTR for the whole composable to prevent emoji/text overlap in RTL languages
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (server.IsAuto()) {
                    Box(modifier = Modifier.size(width = 40.dp, height = 62.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Primary),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Text(
                        text = server.name,
                        color = Primary,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 5.dp),
                    )
                    return@Row
                }
                Box(modifier = Modifier.size(width = 32.dp, height = 38.dp), contentAlignment = Alignment.Center) {
                    val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode)
                    if (!flag.isNullOrEmpty()) {
                        Text(text = flag, fontSize = 20.sp, textAlign = TextAlign.Center)
                    }
                }
                Text(
                    text = server.name,
                    color = Primary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp),
                )
                if (server.isCensured) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Primary),
                        modifier = Modifier
                            .padding(start = 4.dp, end = 20.dp)
                            .size(40.dp),
                    )
                }
            }
            val ping = if (server.IsAuto()) 0L else pingValue
            if (ping != 0L) {
                Row(modifier = Modifier.padding(start = 38.dp)) {
                    if (ping > 0) {
                        Text(text = pingEmoji(ping), fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(text = "${ping}ms", color = Primary, fontSize = 12.sp)
                    } else {
                        Text(text = "---  ---  ---", color = Primary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun pingEmoji(ping: Long): String = when {
    ping < 150 -> "🟢"
    ping < 200 -> "🟡"
    ping < 300 -> "🟠"
    else -> "🔴"
}
