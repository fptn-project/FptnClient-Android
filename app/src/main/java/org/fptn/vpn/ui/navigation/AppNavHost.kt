package org.fptn.vpn.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.fptn.vpn.enums.ConnectionState
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.services.tile.FptnTileService
import org.fptn.vpn.ui.backup.BackupSettingsScreen
import org.fptn.vpn.ui.bypassmethod.BypassMethodsScreen
import org.fptn.vpn.ui.experimentalsettings.ExperimentalSettingsScreen
import org.fptn.vpn.ui.home.HomeScreen
import org.fptn.vpn.ui.login.LoginScreen
import org.fptn.vpn.ui.logs.LogsScreen
import org.fptn.vpn.ui.perappvpn.PerAppVpnModeScreen
import org.fptn.vpn.ui.servers.ServersScreen
import org.fptn.vpn.ui.settings.SettingsScreen
import org.fptn.vpn.ui.splash.SplashRoute
import org.fptn.vpn.ui.splash.SplashScreen
import org.fptn.vpn.ui.stats.StatsScreen
import org.fptn.vpn.ui.updatetoken.UpdateTokenScreen

/**
 * Route constants for the single-activity Compose navigation graph.
 *
 * Every screen in the app is a Compose destination here, so screen-to-screen navigation goes
 * straight through `navController.navigate` — there's no other activity in this app to bridge
 * to. `MainActivity.intentForRoute` still exists solely for launch points that don't have a
 * `NavController` to call, e.g. `SniCheckerService`'s notification tap `PendingIntent`.
 */
object AppRoute {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val UPDATE_TOKEN = "update_token"
    const val LOGS = "logs"
    const val PER_APP_VPN_MODE = "per_app_vpn_mode"
    const val BACKUP = "backup"
    const val BYPASS_METHODS = "bypass_methods"
    const val EXPERIMENTAL_SETTINGS = "experimental_settings"
    const val SETTINGS = "settings"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val STATS = "stats"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startRoute: String = AppRoute.SPLASH,
) {
    // The VPN and the SNI search are mutually exclusive background jobs (enforced in
    // FptnService/SniCheckerService's static start methods) — whichever one is actually
    // running owns the screen, no matter how it was started (in-app button, Quick Settings
    // tile, or a notification tap). These observers key on the static LiveData both services
    // already expose, so a state flip anywhere jumps the user to the matching screen even if
    // they're currently elsewhere in the app.
    val vpnState by FptnTileService.getServiceStateMutableLiveData().observeAsState(ConnectionState.DISCONNECTED)
    val sniState by SniCheckerService.getStaticServiceStateLiveData().observeAsState(SniCheckerServiceState.INACTIVE)

    LaunchedEffect(vpnState.isActiveState) {
        if (vpnState.isActiveState) {
            navController.navigate(AppRoute.HOME) { launchSingleTop = true }
        }
    }
    LaunchedEffect(sniState == SniCheckerServiceState.ACTIVE) {
        if (sniState == SniCheckerServiceState.ACTIVE) {
            navController.navigate(AppRoute.BYPASS_METHODS) { launchSingleTop = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        // The screen itself already switches instantly (this is a single Activity; navigating
        // between routes doesn't re-create any window) — the default crossfade/scale transition
        // just added visible lag on top of that with nothing to justify it, so drop it.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(AppRoute.SPLASH) {
            SplashScreen(
                onRouteResolved = { route ->
                    when (route) {
                        SplashRoute.Login -> navController.navigate(AppRoute.LOGIN) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                        SplashRoute.BypassMethods -> navController.navigate(AppRoute.BYPASS_METHODS) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                        SplashRoute.Home -> navController.navigate(AppRoute.HOME) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(AppRoute.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoute.HOME) {
            HomeScreen(
                onNavigateServers = {
                    navController.navigate(AppRoute.SERVERS) { launchSingleTop = true }
                },
                onNavigateStats = {
                    navController.navigate(AppRoute.STATS) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
                onNavigateUpdateToken = {
                    navController.navigate(AppRoute.UPDATE_TOKEN) { launchSingleTop = true }
                },
                onNavigateBypassMethods = {
                    navController.navigate(AppRoute.BYPASS_METHODS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.SERVERS) {
            ServersScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateStats = {
                    navController.navigate(AppRoute.STATS) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.STATS) {
            StatsScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateServers = {
                    navController.navigate(AppRoute.SERVERS) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.UPDATE_TOKEN) {
            // Reachable from Settings/HomeScreen's nav rows.
            UpdateTokenScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onDoneNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) {
                        launchSingleTop = true
                        popUpTo(AppRoute.UPDATE_TOKEN) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoute.LOGS) {
            // Only reachable today from SettingsScreen's nav row.
            LogsScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.PER_APP_VPN_MODE) {
            // Only reachable today from SettingsScreen's nav row.
            PerAppVpnModeScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.BACKUP) {
            // Only reachable today from SettingsScreen's nav row.
            BackupSettingsScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.BYPASS_METHODS) {
            // Reachable from Splash (first-run routing), Settings/HomeScreen's nav rows, and
            // SniCheckerService's notification tap intent.
            BypassMethodsScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.EXPERIMENTAL_SETTINGS) {
            // Only reachable today from SettingsScreen's nav row.
            ExperimentalSettingsScreen(
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateSettings = {
                    navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(AppRoute.SETTINGS) {
            // Reachable from every sub-screen's BottomNavBar, including HomeScreen's.
            SettingsScreen(
                onLoggedOut = {
                    navController.navigate(AppRoute.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(AppRoute.HOME) { launchSingleTop = true }
                },
                onNavigateServers = {
                    navController.navigate(AppRoute.SERVERS) { launchSingleTop = true }
                },
                onNavigateStats = {
                    navController.navigate(AppRoute.STATS) { launchSingleTop = true }
                },
                onNavigateUpdateToken = {
                    navController.navigate(AppRoute.UPDATE_TOKEN) { launchSingleTop = true }
                },
                onNavigateBypassMethods = {
                    navController.navigate(AppRoute.BYPASS_METHODS) { launchSingleTop = true }
                },
                onNavigatePerAppVpnMode = {
                    navController.navigate(AppRoute.PER_APP_VPN_MODE) { launchSingleTop = true }
                },
                onNavigateExperimentalSettings = {
                    navController.navigate(AppRoute.EXPERIMENTAL_SETTINGS) { launchSingleTop = true }
                },
                onNavigateLogs = {
                    navController.navigate(AppRoute.LOGS) { launchSingleTop = true }
                },
                onNavigateBackup = {
                    navController.navigate(AppRoute.BACKUP) { launchSingleTop = true }
                },
            )
        }
    }
}
