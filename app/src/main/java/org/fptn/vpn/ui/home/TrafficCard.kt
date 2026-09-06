package org.fptn.vpn.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.White

/**
 * The connected-state traffic card: current speed row, an optional live speed chart, and a
 * running-totals row. Extracted out of `HomeScreen` since it's a visually self-contained unit.
 */
@Composable
fun TrafficCard(
    downloadSpeed: String,
    uploadSpeed: String,
    downloadTraffic: String,
    uploadTraffic: String,
    showChart: Boolean,
    speedSample: LongArray?,
    modifier: Modifier = Modifier,
) {
    val dividerColor = Color(0x26FFFFFF)
    Column(
        modifier = modifier
            .legacyDrawableBackground(R.drawable.round_settings_back_white10_20)
            .heightIn(min = 140.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.traffic_icon_size))
                        .padding(end = 8.dp),
                )
                Text(
                    text = downloadSpeed,
                    color = White,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
            }
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(dividerColor))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uploadSpeed,
                    color = White,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Image(
                    painter = painterResource(R.drawable.upload),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.traffic_icon_size))
                        .padding(start = 8.dp),
                )
            }
        }

        if (showChart) {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            val lastAppliedSample = remember { mutableStateOf<LongArray?>(null) }
            AndroidView(
                factory = { ctx -> TrafficSpeedChart(ctx, null) },
                update = { view ->
                    val sample = speedSample
                    if (sample != null && sample !== lastAppliedSample.value) {
                        view.addSample(sample[0], sample[1])
                        lastAppliedSample.value = sample
                    }
                },
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp), horizontalAlignment = Alignment.End) {
                Text(text = stringResource(R.string.traffic_download), color = Color(0x80FFFFFF), fontSize = 10.sp)
                Text(text = downloadTraffic, color = White, fontSize = 14.sp)
            }
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(dividerColor))
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(text = stringResource(R.string.traffic_upload), color = Color(0x80FFFFFF), fontSize = 10.sp)
                Text(text = uploadTraffic, color = White, fontSize = 14.sp)
            }
        }
    }
}
