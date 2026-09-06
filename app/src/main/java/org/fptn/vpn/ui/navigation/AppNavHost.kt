package org.fptn.vpn.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.fptn.vpn.ui.backup.BackupSettingsScreen
import org.fptn.vpn.ui.bypassmethod.BypassMethodsScreen
import org.fptn.vpn.ui.experimentalsettings.ExperimentalSettingsScreen
import org.fptn.vpn.ui.home.HomeScreen
import org.fptn.vpn.ui.login.LoginScreen
import org.fptn.vpn.ui.logs.LogsScreen
import org.fptn.vpn.ui.perappvpn.PerAppVpnModeScreen
import org.fptn.vpn.ui.settings.SettingsScreen
import org.fptn.vpn.ui.splash.SplashRoute
import org.fptn.vpn.ui.splash.SplashScreen
import org.fptn.vpn.ui.updatetoken.UpdateTokenScreen

/**
 * Route constants for the single-activity Compose navigation graph.
 *
 * Every screen in the app is now a Compose destination here. `MainActivity` is
 * `singleTop`, so most screen-to-screen navigation still goes through
 * `MainActivity.intentForRoute` (the "reverse bridge") rather than calling
 * `navController.navigate` directly — that keeps every entry point (a fresh launch, a
 * notification tap, a tile click, a screen already hosted here) going through the same
 * code path instead of assuming an existing `NavController` reference is safe to use.
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
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startRoute: String = AppRoute.SPLASH,
) {
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
            HomeScreen()
        }
        composable(AppRoute.UPDATE_TOKEN) {
            // Only reachable today via the reverse bridge from SettingsScreen/HomeScreen
            // (nothing in this graph links to it yet).
            UpdateTokenScreen()
        }
        composable(AppRoute.LOGS) {
            // Only reachable today via the reverse bridge from SettingsScreen.
            LogsScreen()
        }
        composable(AppRoute.PER_APP_VPN_MODE) {
            // Only reachable today via the reverse bridge from SettingsScreen.
            PerAppVpnModeScreen()
        }
        composable(AppRoute.BACKUP) {
            // Only reachable today via the reverse bridge from SettingsScreen.
            BackupSettingsScreen()
        }
        composable(AppRoute.BYPASS_METHODS) {
            // Reachable from Splash (first-run routing), the reverse bridge from
            // SettingsScreen/HomeScreen, and SniCheckerService's notification tap intent.
            BypassMethodsScreen()
        }
        composable(AppRoute.EXPERIMENTAL_SETTINGS) {
            // Only reachable today via the reverse bridge from SettingsScreen.
            ExperimentalSettingsScreen()
        }
        composable(AppRoute.SETTINGS) {
            // Reachable via the reverse bridge from every sub-screen's BottomNavBar, including
            // HomeScreen's.
            SettingsScreen(
                onLoggedOut = {
                    navController.navigate(AppRoute.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
