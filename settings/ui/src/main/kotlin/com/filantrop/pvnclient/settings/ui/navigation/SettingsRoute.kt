package com.filantrop.pvnclient.settings.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions

data object SettingsRoute // route to Settings screen

const val SETTINGS_ROUTE = "settingsRoute"

fun NavController.navigateToSettings(navOptions: NavOptions) = navigate(route = SettingsRoute, navOptions)
