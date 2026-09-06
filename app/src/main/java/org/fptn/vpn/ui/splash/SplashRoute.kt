package org.fptn.vpn.ui.splash

/**
 * Destination the app lands on once the splash has determined the user's state
 * (are servers present? is the SNI checker running?).
 */
sealed interface SplashRoute {
    data object Login : SplashRoute
    data object Home : SplashRoute
    data object BypassMethods : SplashRoute
}
