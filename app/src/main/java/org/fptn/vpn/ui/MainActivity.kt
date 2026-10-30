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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.fptn.vpn.core.common.Constants
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.ui.theme.FontScaleState

/**
 * The single entry-point activity hosting the whole Compose UI. Every screen is a Compose
 * destination in [AppNavHost], and screen-to-screen navigation goes straight through the
 * `NavController` — no [intentForRoute] involved.
 *
 * Two ways in:
 * - Normal launch: starts at [AppRoute.SPLASH], which resolves the user's destination and
 *   navigates within the same [AppNavHost].
 * - External launch: something outside the Compose graph that has no `NavController` to call —
 *   today just `SniCheckerService`'s notification tap `PendingIntent` — builds an [intentForRoute]
 *   Intent to open a specific route directly. Since this activity is `singleTop`, that Intent is
 *   usually delivered to [onNewIntent] instead of a fresh `onCreate`, and is forwarded into the
 *   existing `NavController`.
 */
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        val requestedRoute = intent.getStringExtra(EXTRA_ROUTE)
        val startRoute = requestedRoute?.let { validateRouteForSession(it) } ?: AppRoute.SPLASH
        setContent {
            navController = rememberNavController()
            FptnTheme {
                // Read the reactive fields from FontScaleState.
                // Compose automatically subscribes to changes:
                // when FontScaleState.setUseSystem() / setIndex() is called from Settings,
                // the entire UI is immediately recomposed with the new fontScale, without restarting the app.
                val systemDensity = LocalDensity.current
                val effectiveDensity = FontScaleState.effectiveFontScaleOrNull()?.let { fs ->
                    Density(
                        density = systemDensity.density,
                        fontScale = fs,
                    )
                } ?: systemDensity

                CompositionLocalProvider(LocalDensity provides effectiveDensity) {
                    AppNavHost(
                        navController = navController,
                        startRoute = startRoute,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // This activity's `singleTop` launch mode delivers an intentForRoute Intent here
        // (instead of a fresh onCreate) whenever it's already on top, e.g. the app is already
        // open when the SNI-check notification is tapped. Forward the route into the existing
        // NavHost instead of silently dropping it.
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: return
        // Same session gate as onCreate: never navigate to an authenticated route when the
        // user is not signed in. Force-route to Login instead so the Compose back stack ends up
        // at the right destination even if the app was reopened from a stale notification.
        val validatedRoute = validateRouteForSession(route)
        if (validatedRoute == null) {
            navController.navigate(AppRoute.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            return
        }
        navController.navigate(validatedRoute) {
            launchSingleTop = true
        }
    }

    /**
     * Returns the route to actually start on, given a route requested via [EXTRA_ROUTE]:
     * - `null` if the requested route requires an authenticated session and the DB is empty
     *   (caller should fall back to [AppRoute.SPLASH] or force-route to [AppRoute.LOGIN]).
     * - The same route otherwise.
     *
     * Public routes (Login, Splash) are always allowed; everything else requires both a server
     * and a token in the DB.
     */
    private fun validateRouteForSession(route: String): String? {
        // Public destinations — always allowed.
        if (route == AppRoute.SPLASH || route == AppRoute.LOGIN) return route

        // Strict 2s timeout: if Room is somehow stuck, fall back to "session invalid" rather than
        // hanging onCreate/onNewIntent and triggering ANR. Splash will then route the user
        // correctly once it can read the DB itself.
        //
        // runBlocking creates the coroutine that withTimeoutOrNull needs to suspend from;
        // withContext switches to Dispatchers.IO for the synchronous Room queries (AppDatabase
        // forbids main-thread queries).
        val (servers, tokens) = runBlocking {
            withTimeoutOrNull(2_000L) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(this@MainActivity)
                    Pair(db.serverDAO().getCountSync(), db.tokenDAO().getCount())
                }
            }
        } ?: Pair(0, 0)
        return if (servers > 0 && tokens > 0) route else null
    }

    companion object {
        private const val EXTRA_ROUTE = "route"

        /**
         * Intent for launch points outside the Compose graph — with no `NavController` to call
         * directly — to open a specific [AppRoute] (e.g. `SniCheckerService`'s notification tap
         * `PendingIntent`).
         */
        @JvmStatic
        fun intentForRoute(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ROUTE, route)
    }
}
