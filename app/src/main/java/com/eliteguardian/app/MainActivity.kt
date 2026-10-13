package com.eliteguardian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eliteguardian.app.navigation.Screen
import com.eliteguardian.app.ui.apps.AppBlockerScreen
import com.eliteguardian.app.ui.apps.AppBlockerViewModel
import com.eliteguardian.app.ui.blocklist.BlocklistScreen
import com.eliteguardian.app.ui.blocklist.BlocklistViewModel
import com.eliteguardian.app.ui.dashboard.DashboardScreen
import com.eliteguardian.app.ui.dashboard.DashboardViewModel
import com.eliteguardian.app.ui.focus.FocusScreen
import com.eliteguardian.app.ui.focus.FocusViewModel
import com.eliteguardian.app.ui.keywords.KeywordScreen
import com.eliteguardian.app.ui.keywords.KeywordViewModel
import com.eliteguardian.app.ui.onboarding.OnboardingScreen
import com.eliteguardian.app.ui.settings.SettingsScreen
import com.eliteguardian.app.ui.settings.SettingsViewModel
import com.eliteguardian.app.ui.theme.EliteGuardianTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val blocklistViewModel: BlocklistViewModel by viewModels()
    private val appBlockerViewModel: AppBlockerViewModel by viewModels()
    private val keywordViewModel: KeywordViewModel by viewModels()
    private val focusViewModel: FocusViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appPreferences = (application as EliteGuardianApp).appPreferences

        setContent {
            EliteGuardianTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isOnboardingDone by appPreferences.isOnboardingCompleted.collectAsState(initial = null)

                    if (isOnboardingDone != null) {
                        val startDestination = if (isOnboardingDone == true) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable(Screen.Onboarding.route) {
                                OnboardingScreen(
                                    onFinished = {
                                        lifecycleScope.launch {
                                            appPreferences.setOnboardingCompleted(true)
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToBlocklist = { navController.navigate(Screen.Blocklist.route) },
                                    onNavigateToAppBlocker = { navController.navigate(Screen.AppBlocker.route) },
                                    onNavigateToKeywordBlocker = { navController.navigate(Screen.KeywordBlocker.route) },
                                    onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                )
                            }
                            composable(Screen.Blocklist.route) {
                                BlocklistScreen(
                                    viewModel = blocklistViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.AppBlocker.route) {
                                AppBlockerScreen(
                                    viewModel = appBlockerViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.KeywordBlocker.route) {
                                KeywordScreen(
                                    viewModel = keywordViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Focus.route) {
                                FocusScreen(
                                    viewModel = focusViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
