package org.fptn.vpn.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.fptn.vpn.R

/**
 * Compose wrapper around the app's real `BottomNavigationView` (Home / Settings / Share),
 * reproducing `CustomBottomNavigationListener`'s navigation behavior. Kept as an `AndroidView`
 * rather than a Compose `NavigationBar`, so the selected/unselected item styling that comes
 * from the app's Material theme stays pixel-identical instead of being re-derived.
 *
 * [isHomeScreen]/[isSettingsScreen] mirror the legacy `context instanceof HomeActivity` /
 * `context instanceof SettingsActivity` checks that predate the Compose port: every screen
 * under Settings (Logs, PerAppVpnMode, UpdateToken, Backup, BypassMethods,
 * ExperimentalSettings) shows the Settings tab as selected without *being* the Settings
 * screen, so re-tapping "Settings" from one of them must still navigate to the real Settings
 * screen.
 *
 * [settingsEnabled] mirrors `HomeActivity`'s `settingsMenuItem.setEnabled(!activeState)`: Home
 * disables the Settings tab while a VPN connection is active/connecting, so the user can't
 * navigate away mid-connection. Every other screen leaves it at the default (always enabled),
 * since only Home ever disabled it.
 */
@Composable
fun BottomNavBar(
    isHomeScreen: Boolean,
    isSettingsScreen: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    settingsEnabled: Boolean = true,
) {
    val selectedItemId = if (isHomeScreen) R.id.menuHome else R.id.menuSettings

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            BottomNavigationView(context).apply {
                inflateMenu(R.menu.bottom_nav_bar_menu)
            }
        },
        update = { view ->
            view.selectedItemId = selectedItemId
            view.menu.findItem(R.id.menuSettings)?.isEnabled = settingsEnabled
            view.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    selectedItemId -> true
                    R.id.menuHome -> {
                        onNavigateHome()
                        true
                    }
                    R.id.menuSettings -> {
                        onNavigateSettings()
                        true
                    }
                    R.id.menuShare -> {
                        onShare()
                        false
                    }
                    else -> false
                }
            }
            view.setOnItemReselectedListener { item ->
                when (item.itemId) {
                    R.id.menuSettings -> if (!isSettingsScreen) onNavigateSettings()
                    R.id.menuHome -> if (!isHomeScreen) onNavigateHome()
                }
            }
        },
    )
}
