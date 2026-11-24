package org.fptn.vpn.ui.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.NavigationTab
import org.fptn.vpn.ui.theme.LocalThemeState
import org.fptn.vpn.ui.theme.MetricTypography
import org.fptn.vpn.ui.theme.PingGood
import org.fptn.vpn.ui.theme.PingMedium
import org.fptn.vpn.ui.theme.PingPoor
import org.fptn.vpn.ui.theme.cardContainerColor

/**
 * Material Design 3 Expressive ServersScreen for FptnClient.
 * Displays expandable subscription/token profiles, filter chips,
 * latency tests, and server cards with active node highlights.
 */
@Composable
fun ServersScreen(
    onNavigateHome: () -> Unit = {},
    onNavigateStats: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    viewModel: ServersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState = LocalThemeState.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "pingRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "rotationAngle",
    )

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

            // 1. Top Bar: Header & Add Token action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Серверы",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { viewModel.openAddTokenSheet() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Токен",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Expandable Subscription / Token Profile Header Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row (Active profile summary + toggle expand arrow)
                    val activeProfile = uiState.selectedProfile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleProfilesExpanded() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = activeProfile?.emoji ?: "🔥", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProfile?.title ?: "FPTN Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Обновлено: ${activeProfile?.updatedAt ?: "Недавно"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Icon(
                            painter = painterResource(
                                if (uiState.isProfilesExpanded) {
                                    R.drawable.spinner_arrow_up
                                } else {
                                    R.drawable.spinner_arrow_down
                                },
                            ),
                            contentDescription = "Toggle profiles",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Progress bar & limits
                    if (activeProfile != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { activeProfile.trafficProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${activeProfile.trafficUsed} / ${activeProfile.trafficLimit}",
                                style = MetricTypography.Traffic,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = activeProfile.daysLeftText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Expanded profile list
                    AnimatedVisibility(visible = uiState.isProfilesExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.profiles.forEach { profile ->
                                val isSelected = profile.isSelected
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectProfile(profile.id) },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(text = profile.emoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                            Text(
                                                text = "${profile.trafficUsed} / ${profile.trafficLimit} • ${profile.daysLeftText}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_check_16),
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Add new profile button inside expanded container
                            OutlinedButton(
                                onClick = { viewModel.openAddTokenSheet() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(percent = 50),
                            ) {
                                Text("+ Добавить подписку или конфиг")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Filter / Sort Pills & Ping check button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Node count chip
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.cardContainerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = "🌐 Узлов: ${uiState.servers.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }

                    // Sort chip (cycles default -> fastest -> available)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.cardContainerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            val next = when (uiState.sortFilter) {
                                ServerSortFilter.DEFAULT -> ServerSortFilter.FASTEST
                                ServerSortFilter.FASTEST -> ServerSortFilter.AVAILABLE
                                ServerSortFilter.AVAILABLE -> ServerSortFilter.DEFAULT
                            }
                            viewModel.setSortFilter(next)
                        },
                    ) {
                        Text(
                            text = "≡ ${uiState.sortFilter.label}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }

                // Check Ping Action Button
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { viewModel.checkPing() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_update_24),
                            contentDescription = "Check Ping",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .then(
                                    if (uiState.isCheckingPing) {
                                        Modifier.rotate(rotationAngle)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isCheckingPing) "Тест..." else "Пинг",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Server Cards List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.displayServers.forEach { server ->
                    val isSelected = server.isSelected
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.cardContainerColor
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (themeState.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.selectServer(server.id)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Flag badge container
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = server.flag, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Server metadata
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = "АКТИВЕН",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${server.region} • ${server.protocol}:${server.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }

                            // Tabular latency readout pill
                            val pingColor = when {
                                server.pingMs < 80 -> PingGood
                                server.pingMs < 150 -> PingMedium
                                else -> PingPoor
                            }

                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = pingColor.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "${server.pingMs} мс",
                                    style = MetricTypography.Latency,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pingColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Add Token Dialog
        if (uiState.showAddTokenSheet) {
            AlertDialog(
                onDismissRequest = { viewModel.closeAddTokenSheet() },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Добавить FPTN токен",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Вставьте строку токена или ссылку на профиль подключения:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.tokenInputText,
                            onValueChange = { viewModel.updateTokenInput(it) },
                            placeholder = { Text("https://... или fptn://...") },
                            isError = uiState.tokenInputError != null,
                            supportingText = uiState.tokenInputError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text.orEmpty()
                                if (clip.isNotBlank()) {
                                    viewModel.updateTokenInput(clip)
                                }
                            },
                        ) {
                            Text("📋 Вставить из буфера обмена")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.submitToken() },
                        shape = RoundedCornerShape(percent = 50),
                    ) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeAddTokenSheet() }) {
                        Text("Отмена")
                    }
                },
            )
        }

        // Bottom Navigation Bar
        BottomNavBar(
            currentTab = NavigationTab.SERVERS,
            onNavigateHome = onNavigateHome,
            onNavigateServers = {},
            onNavigateStats = onNavigateStats,
            onNavigateSettings = onNavigateSettings,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
