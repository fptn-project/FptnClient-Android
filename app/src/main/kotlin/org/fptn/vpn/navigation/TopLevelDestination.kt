package org.fptn.vpn.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.filantrop.pvnclient.home.ui.navigation.HOME_BASE_ROUTE
import com.filantrop.pvnclient.home.ui.navigation.HOME_ROUTE
import com.filantrop.pvnclient.settings.ui.navigation.SETTINGS_ROUTE
import org.fptn.vpn.R
import org.fptn.vpn.core.designsystem.icons.PvnIcons

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
    val route: String,
    val baseRoute: String = route,
) {
    HOME(
        selectedIcon = PvnIcons.Home,
        unselectedIcon = PvnIcons.HomeBorder,
        iconTextId = R.string.home_title,
        titleTextId = R.string.home_title,
        route = HOME_ROUTE,
        baseRoute = HOME_BASE_ROUTE,
    ),
    SETTINGS(
        selectedIcon = PvnIcons.Settings,
        unselectedIcon = PvnIcons.SettingsBorder,
        iconTextId = R.string.settings_title,
        titleTextId = R.string.settings_title,
        route = SETTINGS_ROUTE,
    ),
}
