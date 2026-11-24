package org.fptn.vpn.ui.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.NavigationTab
import org.fptn.vpn.ui.theme.AppPalette
import org.fptn.vpn.ui.theme.LocalThemeController
import org.fptn.vpn.ui.theme.LocalThemeState
import org.fptn.vpn.ui.theme.MetricTypography
import org.fptn.vpn.ui.theme.PingGood
import org.fptn.vpn.ui.theme.StatusConnected
import org.fptn.vpn.ui.theme.StatusConnecting
import org.fptn.vpn.ui.theme.StatusDisconnected
import org.fptn.vpn.ui.theme.ThemeMode
import org.fptn.vpn.ui.theme.cardContainerColor
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated dual sinusoidal wave drawn on Compose Canvas.
 * Active when VPN is connected, matching the visualizer in DESIGN.md and references.
 */
@Composable
fun AnimatedSineWave(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseAnimation",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitudePrimary = height * 0.28f
        val amplitudeSecondary = height * 0.18f
        val wavelength = width / 2.2f

        // Secondary subtle background wave
        val secondaryPath = Path()
        val step = 4f
        var x = 0f
        secondaryPath.moveTo(0f, centerY)
        while (x <= width) {
            val relativeX = x / wavelength
            val y = centerY + amplitudeSecondary * sin((relativeX * 2 * PI + phase * 1.3f).toDouble()).toFloat()
            secondaryPath.lineTo(x, y)
            x += step
        }
        drawPath(
            path = secondaryPath,
            color = waveColor.copy(alpha = 0.4f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        // Primary expressive wave
        val primaryPath = Path()
        x = 0f
        primaryPath.moveTo(0f, centerY)
        while (x <= width) {
            val relativeX = x / wavelength
            val y = centerY + amplitudePrimary * sin((relativeX * 2 * PI + phase).toDouble()).toFloat()
            primaryPath.lineTo(x, y)
            x += step
        }
        drawPath(
            path = primaryPath,
            color = waveColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * Material Design 3 Expressive HomeScreen for FptnClient.
 * Fully decoupled from real VPN tunnels; powered by [HomeViewModel] mock UiState.
 */
@Composable
fun HomeScreen(
    onNavigateServers: () -> Unit = {},
    onNavigateStats: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateUpdateToken: () -> Unit = {},
    onNavigateBypassMethods: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState = LocalThemeState.current
    val themeController = LocalThemeController.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadActiveServer()
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpn()
        }
    }

    val handleConnectClick = {
        if (themeState.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (uiState.connectionState == ConnectionState.DISCONNECTED) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.startVpn()
            }
        } else {
            viewModel.disconnectVpn()
        }
    }

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
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Top Bar / App Header with Palette & Theme toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logo_24),
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "FPTN VPN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Palette cycle chip (Lilac -> Forest -> Ocean)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextPalette = when (themeState.palette) {
                                    AppPalette.LILAC -> AppPalette.FOREST
                                    AppPalette.FOREST -> AppPalette.OCEAN
                                    AppPalette.OCEAN -> AppPalette.LILAC
                                }
                                themeController.setPalette(nextPalette)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_experimental_features_24),
                                contentDescription = "Switch Palette",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Theme Mode toggle (Light / Dark)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextMode = if (themeState.themeMode == ThemeMode.DARK) {
                                    ThemeMode.LIGHT
                                } else {
                                    ThemeMode.DARK
                                }
                                themeController.setThemeMode(nextMode)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_settings_24),
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Dismissible Notification / Announcement Card
            if (uiState.isNoticeVisible) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.bell_ring_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.noticeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.noticeMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = "Dismiss notice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.dismissNotice() },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Greeting Header (Time-based: Доброе утро / день / вечер / ночь)
            Text(
                text = "FPTN Безопасность",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Status / Timer / Ping Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status Pill
                val statusDotColor = when (uiState.connectionState) {
                    ConnectionState.CONNECTED -> StatusConnected
                    ConnectionState.CONNECTING -> StatusConnecting
                    else -> StatusDisconnected
                }
                val statusLabel = when (uiState.connectionState) {
                    ConnectionState.CONNECTED -> "Подключено"
                    ConnectionState.CONNECTING -> "Подключение..."
                    else -> "Отключено"
                }
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = statusDotColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Session Timer Pill (visible when connected/connecting)
                if (uiState.connectionState != ConnectionState.DISCONNECTED) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.cardContainerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "⏱ ",
                                fontSize = 12.sp,
                            )
                            Text(
                                text = uiState.sessionTimerText,
                                style = MetricTypography.Timer,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // Ping Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.cardContainerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = PingGood)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.pingMs} мс",
                            style = MetricTypography.Latency,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Active Server Card (Clickable to open ServersScreen)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateServers() },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Flag Container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.serverFlag,
                            fontSize = 24.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Активный сервер",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = uiState.serverProtocol.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.serverName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${uiState.serverRegion} • ${uiState.pingMs} мс",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                        contentDescription = "Choose server",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Large Interactive Connect/Wave Capsule
            when (uiState.connectionState) {
                ConnectionState.CONNECTED -> {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                handleConnectClick()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedSineWave(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 24.dp),
                                waveColor = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Подключено • Нажмите для отключения",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                            )
                        }
                    }
                }

                ConnectionState.CONNECTING -> {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                handleConnectClick()
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Подключение к серверу...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                handleConnectClick()
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.toggle_button_on),
                                contentDescription = "Connect",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Подключиться",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Telemetry & Data Traffic Summary Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    // Download
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Загрузка",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.connectionState == ConnectionState.CONNECTED) uiState.downloadSpeed else "0 B/s",
                            style = MetricTypography.HeroCounter,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Всего: ${uiState.downloadTraffic}",
                            style = MetricTypography.Traffic,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )

                    // Upload
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.upload),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Отдача",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.connectionState == ConnectionState.CONNECTED) uiState.uploadSpeed else "0 B/s",
                            style = MetricTypography.HeroCounter,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Всего: ${uiState.uploadTraffic}",
                            style = MetricTypography.Traffic,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Service Status Chips
            Text(
                text = "Доступность сервисов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.services.take(2).forEach { service ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.cardContainerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = if (service.isAvailable) StatusConnected else StatusDisconnected)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${service.pingMs} мс",
                                    style = MetricTypography.Latency,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Bottom space clearance for navigation bar
            Spacer(modifier = Modifier.height(110.dp))
        }

        // Bottom Navigation Bar
        BottomNavBar(
            currentTab = NavigationTab.HOME,
            onNavigateHome = {},
            onNavigateServers = onNavigateServers,
            onNavigateStats = onNavigateStats,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
