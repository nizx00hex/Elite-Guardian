package com.eliteguardian.app.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Blocklist : Screen("blocklist", "Blocklist")
    object AppBlocker : Screen("app_blocker", "App Blocker")
    object KeywordBlocker : Screen("keyword_blocker", "Keyword Blocker")
    object Focus : Screen("focus", "Focus Mode")
    object Streak : Screen("streak", "Streak Tracker")
    object SearchRestrictions : Screen("search_restrictions", "Search Restrictions")
    object Settings : Screen("settings", "Settings")
}
