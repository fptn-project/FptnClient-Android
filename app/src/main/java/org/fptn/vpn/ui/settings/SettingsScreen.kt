package org.fptn.vpn.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.enums.BypassCensorshipMethod
import org.fptn.vpn.enums.ConnectionStrategy
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.NavigationTab
import org.fptn.vpn.ui.theme.AmoledBackground
import org.fptn.vpn.ui.theme.AmoledCardContainer
import org.fptn.vpn.ui.theme.AppPalette
import org.fptn.vpn.ui.theme.ContrastLevel
import org.fptn.vpn.ui.theme.ForestDarkPrimary
import org.fptn.vpn.ui.theme.LilacDarkPrimary
import org.fptn.vpn.ui.theme.LocalThemeController
import org.fptn.vpn.ui.theme.LocalThemeState
import org.fptn.vpn.ui.theme.MetricTypography
import org.fptn.vpn.ui.theme.NavBarStyle
import org.fptn.vpn.ui.theme.OceanDarkPrimary
import org.fptn.vpn.ui.theme.PingGood
import org.fptn.vpn.ui.theme.StatusConnected
import org.fptn.vpn.ui.theme.ThemeMode
import org.fptn.vpn.ui.theme.cardContainerColor
import org.fptn.vpn.utils.startIfResolvable

/**
 * Material Design 3 Expressive SettingsScreen for FptnClient.
 * Fully grouped into rounded cards (22.dp) covering Appearance, Bypass,
 * Network/DNS Reliability, and System/Diagnostics.
 */
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateServers: () -> Unit = {},
    onNavigateStats: () -> Unit = {},
    onNavigateUpdateToken: () -> Unit = {},
    onNavigateBypassMethods: () -> Unit = {},
    onNavigatePerAppVpnMode: () -> Unit = {},
    onNavigateExperimentalSettings: () -> Unit = {},
    onNavigateLogs: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState = LocalThemeState.current
    val themeController = LocalThemeController.current
    val context = LocalContext.current

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

            // Screen Header
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ================================================================
            // 1. Внешний вид и тема (Appearance & Theme)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAppearanceExpanded() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_experimental_features_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Оформление и тема",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            painter = painterResource(
                                if (uiState.isAppearanceExpanded) {
                                    R.drawable.spinner_arrow_up
                                } else {
                                    R.drawable.spinner_arrow_down
                                },
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    AnimatedVisibility(visible = uiState.isAppearanceExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Mini Phone Preview Mockup
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (themeState.amoledMode && themeState.themeMode != ThemeMode.LIGHT) {
                                    AmoledBackground
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "FPTN VPN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "42 мс",
                                            style = MetricTypography.Latency,
                                            fontSize = 10.sp,
                                            color = PingGood,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(28.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Подключено ~~~~~",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                }
                            }

                            // Theme Mode Selector (Auto / Light / Dark)
                            Text(
                                text = "Режим темы",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ThemeMode.values().forEach { mode ->
                                    val isSelected = themeState.themeMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .clickable { themeController.setThemeMode(mode) },
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = mode.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pure Black AMOLED Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Чисто чёрный AMOLED",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "#000000 фон для экономии энергии",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = themeState.amoledMode,
                                    onCheckedChange = { themeController.setAmoledMode(it) },
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Accent Palettes Picker (Lilac, Forest, Ocean)
                            Text(
                                text = "Цветовая палитра",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                val palettes = listOf(
                                    Triple(AppPalette.LILAC, LilacDarkPrimary, "Сирень"),
                                    Triple(AppPalette.FOREST, ForestDarkPrimary, "Лес"),
                                    Triple(AppPalette.OCEAN, OceanDarkPrimary, "Океан"),
                                )
                                palettes.forEach { (palette, color, name) ->
                                    val isSelected = themeState.palette == palette
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { themeController.setPalette(palette) },
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check_16),
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contrast Level Selector
                            Text(
                                text = "Уровень контраста",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ContrastLevel.values().forEach { contrast ->
                                    val isSelected = themeState.contrast == contrast
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .clickable { themeController.setContrast(contrast) },
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = contrast.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Navigation Bar Style (Floating vs Docked)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Стиль нижней панели",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (themeState.navBarStyle == NavBarStyle.FLOATING) "Плавающая пилюля" else "Классическая полоса",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = themeState.navBarStyle == NavBarStyle.FLOATING,
                                    onCheckedChange = { isFloating ->
                                        themeController.setNavBarStyle(
                                            if (isFloating) NavBarStyle.FLOATING else NavBarStyle.DOCKED,
                                        )
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Haptic Feedback Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Тактильный отклик (вибрация)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Вибрация при переключении и тапах",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = themeState.hapticEnabled,
                                    onCheckedChange = { themeController.setHapticEnabled(it) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================================================
            // 2. Стратегия туннелирования и обхода (Bypass & Strategy)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_share_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Стратегия и методы обхода",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connection Strategy Selector
                    Text(
                        text = "Стратегия подключения",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            ConnectionStrategy.ROLLING_TUNNEL to "Скользящий",
                            ConnectionStrategy.DOUBLE_ROLLING_TUNNEL to "Двойной",
                            ConnectionStrategy.TRIPLE_ROLLING_TUNNEL to "Тройной",
                        ).forEach { (strategy, name) ->
                            val isSelected = uiState.connectionStrategy == strategy
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { viewModel.setConnectionStrategy(strategy) },
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bypass Method Selector
                    Text(
                        text = "Метод обхода цензуры",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            BypassCensorshipMethod.TLS_OBFUSCATION to "Маскировка TLS",
                            BypassCensorshipMethod.SNI_REALITY to "SNI + REALITY",
                        ).forEach { (method, name) ->
                            val isSelected = uiState.bypassMethod == method
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { viewModel.setBypassMethod(method) },
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SNI Spoofing row & Search SNI action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateBypassMethods() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Маскирующий домен (SNI)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${uiState.currentSni} • Нажмите для проверки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Per-App VPN Mode row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigatePerAppVpnMode() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Раздельное туннелирование",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Исключено ${uiState.excludedAppsCount} приложений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================================================
            // 3. Сеть, DNS и надёжность (Network, DNS & Reliability)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.server_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Сеть, DNS и надёжность",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // AdBlock DNS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Блокировка рекламы и трекеров",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Фильтрация на уровне DNS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.adBlockEnabled,
                            onCheckedChange = { viewModel.setAdBlockEnabled(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom DNS IPv4 toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Пользовательский DNS",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.customDnsEnabled) uiState.customDnsIpv4 else "DNS сервера FPTN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.customDnsEnabled,
                            onCheckedChange = { viewModel.setCustomDnsEnabled(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speed & Traffic in status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Скорость в уведомлении",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Показывать реальную скорость в статус-баре",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.showSpeedInNotification,
                            onCheckedChange = { viewModel.setShowSpeedInNotification(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-fallback on disconnect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Авто-переключение узлов",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Искать рабочий сервер при разрыве",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.autoFallbackEnabled,
                            onCheckedChange = { viewModel.setAutoFallbackEnabled(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kill-switch Info & System Shortcut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startIfResolvable(Intent(Settings.ACTION_VPN_SETTINGS))
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Аварийный выключатель (Kill-switch)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Открыть системные настройки VPN Android >",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Domain blacklist & split tunneling
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateExperimentalSettings() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Расширенные списки доменов",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Черный список и раздельный туннель >",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================================================
            // 4. Система, бэкап и диагностика (System, Backup & Diagnostics)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.cloud_back_up_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Система и диагностика",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Backup & Restore
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateBackup() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Резервное копирование",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Экспорт и импорт JSON настроек",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logs Viewer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateLogs() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Системный журнал (Логи)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Просмотр событий и ошибок",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_arrow_forward_ios_16),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // App Version & Updates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "FPTN Client",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Версия ${uiState.appVersion} (${uiState.buildDate})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { viewModel.checkUpdates() },
                        ) {
                            Text(
                                text = if (uiState.isCheckingUpdates) "Проверка..." else "Обновления",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Logout / Reset
                    Button(
                        onClick = { viewModel.showLogoutDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Выйти из аккаунта",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Logout Confirmation Dialog
        if (uiState.showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.hideLogoutDialog() },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Выход из аккаунта") },
                text = { Text("Вы уверены, что хотите выйти? Все сохранённые серверы будут удалены.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllServers()
                            viewModel.hideLogoutDialog()
                            onLoggedOut()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        shape = RoundedCornerShape(percent = 50),
                    ) {
                        Text("Выйти")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideLogoutDialog() }) {
                        Text("Отмена")
                    }
                },
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            currentTab = NavigationTab.SETTINGS,
            onNavigateHome = onNavigateHome,
            onNavigateServers = onNavigateServers,
            onNavigateStats = onNavigateStats,
            onNavigateSettings = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
