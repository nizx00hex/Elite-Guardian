package com.eliteguardian.app.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Blocklist : Screen("blocklist", "Blocklist")
    object Focus : Screen("focus", "Focus Mode")
    object Settings : Screen("settings", "Settings")
}

