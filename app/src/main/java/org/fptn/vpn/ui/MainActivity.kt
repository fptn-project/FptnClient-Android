package org.fptn.vpn.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.fptn.vpn.ui.navigation.AppNavHost
import org.fptn.vpn.ui.navigation.AppRoute
import org.fptn.vpn.ui.theme.FptnTheme

/**
 * The single entry-point activity hosting the whole Compose UI. Every screen is now a Compose
 * destination in [AppNavHost].
 *
 * Two ways in:
 * - Normal launch: starts at [AppRoute.SPLASH], which resolves the user's destination and
 *   navigates within the same [AppNavHost].
 * - Reverse bridge: a Compose screen already hosted here (e.g. Settings linking to Bypass
 *   Methods, or HomeScreen linking to Settings) launches this activity via [intentForRoute],
 *   which starts directly at that route instead of at the splash. Since this activity is
 *   `singleTop`, that Intent is usually delivered to [onNewIntent] instead of a fresh
 *   `onCreate`, and is forwarded into the existing `NavController` — this keeps every entry
 *   point (a fresh launch, a notification tap, a tile click, a screen already hosted here)
 *   going through the same code path instead of assuming an existing `NavController` reference
 *   is safe to use directly.
 */
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startRoute = intent.getStringExtra(EXTRA_ROUTE) ?: AppRoute.SPLASH
        setContent {
            navController = rememberNavController()
            FptnTheme {
                AppNavHost(
                    navController = navController,
                    startRoute = startRoute,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // This activity's `singleTop` launch mode delivers a reverse-bridge Intent here
        // (instead of a fresh onCreate) whenever it's already on top — which now happens on
        // every navigation between two reverse-bridge screens hosted here, e.g. Settings
        // linking to Bypass Methods. Forward the route into the existing NavHost instead of
        // silently dropping it.
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: return
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    companion object {
        private const val EXTRA_ROUTE = "route"

        /** Intent for a legacy `Activity` to open an already-ported Compose [AppRoute] directly. */
        @JvmStatic
        fun intentForRoute(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ROUTE, route)
    }
}
