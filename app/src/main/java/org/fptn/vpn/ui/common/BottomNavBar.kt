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
 * navigate away mid-connection. [homeEnabled] is the same idea used by BypassMethodsScreen: while
 * SNI auto-select checking is running, both tabs are disabled so the user can't navigate away and
 * lose sight of it (the app also always reopens directly on BypassMethodsScreen while checking is
 * active — see `SplashScreen`'s `sniActive` routing). Every other screen leaves both at the
 * default (always enabled).
 */
@Composable
fun BottomNavBar(
    isHomeScreen: Boolean,
    isSettingsScreen: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    homeEnabled: Boolean = true,
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
            // `BottomNavigationView.setSelectedItemId`/toggling a menu item's `isEnabled` can
            // synchronously redispatch through whichever listener is *currently* attached —
            // and since this lambda only re-registers fresh listeners at the end, an in-between
            // dispatch would run the *previous* recomposition's listener, closed over its stale
            // enabled/screen flags. That's exactly what fired a real "navigate to Settings" the
            // instant `settingsEnabled` flipped to false (SNI checking started): the old,
            // still-enabled listener ran before being replaced. Detach listeners first, so
            // nothing can fire off a mutation below, then reattach fresh ones with current
            // values once the view's state is settled.
            view.setOnItemSelectedListener(null)
            view.setOnItemReselectedListener(null)

            if (view.selectedItemId != selectedItemId) {
                view.selectedItemId = selectedItemId
            }
            view.menu.findItem(R.id.menuHome)?.isEnabled = homeEnabled
            view.menu.findItem(R.id.menuSettings)?.isEnabled = settingsEnabled

            view.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    selectedItemId -> true
                    R.id.menuHome -> {
                        if (homeEnabled) onNavigateHome()
                        true
                    }
                    R.id.menuSettings -> {
                        if (settingsEnabled) onNavigateSettings()
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
                    R.id.menuSettings -> if (!isSettingsScreen && settingsEnabled) onNavigateSettings()
                    R.id.menuHome -> if (!isHomeScreen && homeEnabled) onNavigateHome()
                }
            }
        },
    )
}
