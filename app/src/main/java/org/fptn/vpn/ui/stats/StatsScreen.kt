package org.fptn.vpn.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.NavigationTab
import org.fptn.vpn.ui.theme.MetricTypography
import org.fptn.vpn.ui.theme.PingGood
import org.fptn.vpn.ui.theme.cardContainerColor

/**
 * Material Design 3 Expressive StatsScreen for FptnClient.
 * Features:
 * - Weekly traffic bar chart on Canvas with cap highlights
 * - Live speed telemetry cards with tabular numerals
 * - Packet loss and process memory diagnostic cards
 */
@Composable
fun StatsScreen(
    onNavigateHome: () -> Unit = {},
    onNavigateServers: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    viewModel: StatsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val barNormalColor = MaterialTheme.colorScheme.primaryContainer
    val barHighlightColor = MaterialTheme.colorScheme.primary
    val barCapColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Header with Reset Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Статистика",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = "⏱ с ${uiState.lastResetTime}",
                        style = MetricTypography.Latency,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Weekly Traffic Bar Chart Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_bar_chart_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Эта неделя",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Text(
                            text = "Всего ${uiState.totalWeeklyTraffic}",
                            style = MetricTypography.Traffic,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Canvas Bar Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = uiState.weeklyTraffic.size
                            if (count == 0) return@Canvas

                            val maxVal = uiState.weeklyTraffic.maxOfOrNull { it.volumeGiB } ?: 1f
                            val availableWidth = size.width
                            val availableHeight = size.height - 24.dp.toPx()
                            val barWidth = 28.dp.toPx()
                            val spacing = (availableWidth - (count * barWidth)) / (count + 1)

                            uiState.weeklyTraffic.forEachIndexed { index, day ->
                                val barHeight = ((day.volumeGiB / maxVal) * availableHeight).coerceAtLeast(10.dp.toPx())
                                val startX = spacing + index * (barWidth + spacing)
                                val startY = availableHeight - barHeight

                                // Draw vertical rounded bar
                                drawRoundRect(
                                    color = if (day.isMax) barHighlightColor else barNormalColor,
                                    topLeft = Offset(startX, startY),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                )

                                // Draw accent cap on top
                                drawRoundRect(
                                    color = barCapColor,
                                    topLeft = Offset(startX, startY),
                                    size = Size(barWidth, 6.dp.toPx()),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                )
                            }
                        }

                        // Day labels underneath bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            uiState.weeklyTraffic.forEach { day ->
                                Text(
                                    text = day.dayLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (day.isMax) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isMax) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Live Speed & Telemetry Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Скорость в реальном времени",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "Пик ${uiState.peakSpeed}",
                                style = MetricTypography.Latency,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        // Download column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Входящий",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.currentDownloadSpeed,
                                style = MetricTypography.HeroCounter,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Всего: ${uiState.totalDownloaded}",
                                style = MetricTypography.Traffic,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(56.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        )

                        // Upload column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.upload),
                                    contentDescription = "Upload",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Исходящий",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.currentUploadSpeed,
                                style = MetricTypography.HeroCounter,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Всего: ${uiState.totalUploaded}",
                                style = MetricTypography.Traffic,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Packet Loss & Memory Usage Diagnostics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Packet Loss Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PingGood),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Потери пакетов",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${uiState.packetLossPercent}%",
                            style = MetricTypography.HeroCounter,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Замер каждые 15 с",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Memory Usage Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.eco_battery_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Память процесса",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.memoryUsageMb,
                            style = MetricTypography.HeroCounter,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Потоков: ${uiState.activeThreads}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Bottom Navigation Bar
        BottomNavBar(
            currentTab = NavigationTab.STATS,
            onNavigateHome = onNavigateHome,
            onNavigateServers = onNavigateServers,
            onNavigateStats = {},
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
