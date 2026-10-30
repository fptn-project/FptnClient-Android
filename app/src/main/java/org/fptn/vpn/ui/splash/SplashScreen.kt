package org.fptn.vpn.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fptn.vpn.R
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.services.snichecker.SniCheckerService
import org.fptn.vpn.services.snichecker.SniCheckerServiceState
import org.fptn.vpn.ui.theme.Primary

/**
 * Compose replacement for the old `SplashActivity`.
 *
 * Shows the logo and, in parallel, resolves where the app should continue
 * (login / home / bypass-methods) from the Room server count and the SNI checker
 * state. The resolved destination is delivered to [onRouteResolved].
 */
@Composable
fun SplashScreen(
    onRouteResolved: (SplashRoute) -> Unit,
) {
    val context = LocalContext.current
    var routed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Read both server count and token count — a valid session needs both. Without the
        // token check, Splash would route to Home whenever the server table is non-empty even if
        // the token table is empty (e.g. partial DB migration / manual edit / future logout path
        // that deletes only tokens), exposing the authenticated UI to an unauthenticated user.
        val (serverCount, tokenCount) = withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                Pair(db.serverDAO().getCountSync(), db.tokenDAO().getCount())
            } catch (t: Throwable) {
                XLog.tag("Splash").e("Failed to read session state: %s", t.message)
                Pair(0, 0)
            }
        }
        val sniActive = SniCheckerService.getStaticState() == SniCheckerServiceState.ACTIVE
        val route = when {
            serverCount > 0 && tokenCount > 0 ->
                if (sniActive) SplashRoute.BypassMethods else SplashRoute.Home
            else -> SplashRoute.Login
        }
        XLog.tag("Splash").i("Routing to %s [servers=%d, tokens=%d, sniActive=%b]",
            route, serverCount, tokenCount, sniActive)
        if (!routed) {
            routed = true
            onRouteResolved(route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onRouteResolved = {})
}
