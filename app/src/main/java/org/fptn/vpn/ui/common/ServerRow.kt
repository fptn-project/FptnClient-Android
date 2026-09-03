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
 * Compose port of `ServerEntityAdapter`'s row (`home_list_recycler_server_item.xml`): the
 * "Auto" pseudo-server gets a centered logo + name with no ping/flag/censored row, a real
 * server gets a flag emoji, name, an optional censored icon, and a ping row (colored emoji +
 * "Nms", "---  ---  ---" for unreachable, or nothing if not yet pinged).
 */
@Composable
fun ServerRow(server: ServerEntity, modifier: Modifier = Modifier) {
    if (server.IsAuto()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Primary),
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = server.name,
                color = Primary,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val flag = CountryFlags.getCountryFlagByCountryCode(server.countryCode)
            Box(modifier = Modifier.size(width = 32.dp, height = 38.dp), contentAlignment = Alignment.Center) {
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
                        .padding(start = 4.dp)
                        .size(24.dp),
                )
            }
        }
        val ping = server.pingMs
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

private fun pingEmoji(ping: Long): String = when {
    ping < 150 -> "🟢"
    ping < 200 -> "🟡"
    ping < 300 -> "🟠"
    else -> "🔴"
}
