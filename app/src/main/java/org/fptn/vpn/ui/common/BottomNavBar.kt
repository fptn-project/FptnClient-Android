package org.fptn.vpn.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.fptn.vpn.R
import org.fptn.vpn.ui.theme.LocalThemeState
import org.fptn.vpn.ui.theme.NavBarStyle
import org.fptn.vpn.ui.theme.cardContainerColor

enum class NavigationTab(val label: String, val iconRes: Int) {
    HOME("Главная", R.drawable.ic_baseline_home_24),
    SERVERS("Серверы", R.drawable.server_icon),
    STATS("Статистика", R.drawable.ic_baseline_bar_chart_24),
    SETTINGS("Настройки", R.drawable.ic_baseline_settings_24),
}

/**
 * Modern Material Design 3 Expressive bottom navigation bar.
 * Supports both Floating Pill (default) and Classic Docked styles
 * controlled via [LocalThemeState].
 */
@Composable
fun BottomNavBar(
    currentTab: NavigationTab = NavigationTab.HOME,
    onTabSelected: (NavigationTab) -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateServers: () -> Unit = {},
    onNavigateStats: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeEnabled: Boolean = true,
    settingsEnabled: Boolean = true,
    isHomeScreen: Boolean = (currentTab == NavigationTab.HOME),
    isSettingsScreen: Boolean = (currentTab == NavigationTab.SETTINGS),
) {
    val themeState = LocalThemeState.current
    val haptic = LocalHapticFeedback.current

    val activeTab = when {
        isHomeScreen -> NavigationTab.HOME
        isSettingsScreen -> NavigationTab.SETTINGS
        else -> currentTab
    }

    fun handleTabClick(tab: NavigationTab) {
        if (themeState.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onTabSelected(tab)
        when (tab) {
            NavigationTab.HOME -> if (homeEnabled) onNavigateHome()
            NavigationTab.SERVERS -> onNavigateServers()
            NavigationTab.STATS -> onNavigateStats()
            NavigationTab.SETTINGS -> if (settingsEnabled) onNavigateSettings()
        }
    }

    if (themeState.navBarStyle == NavBarStyle.FLOATING) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.cardContainerColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = tab == activeTab
                        val isEnabled = when (tab) {
                            NavigationTab.HOME -> homeEnabled
                            NavigationTab.SETTINGS -> settingsEnabled
                            else -> true
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                                    .clickable(enabled = isEnabled) { handleTabClick(tab) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = tab.label,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = isEnabled) { handleTabClick(tab) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = tab.label,
                                    tint = if (isEnabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        NavigationBar(
            containerColor = MaterialTheme.cardContainerColor,
            modifier = modifier.fillMaxWidth(),
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == activeTab
                val isEnabled = when (tab) {
                    NavigationTab.HOME -> homeEnabled
                    NavigationTab.SETTINGS -> settingsEnabled
                    else -> true
                }

                NavigationBarItem(
                    selected = isSelected,
                    enabled = isEnabled,
                    onClick = { handleTabClick(tab) },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
