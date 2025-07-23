package com.xptlabs.varliktakibi.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.presentation.analytics.AnalyticsScreen
import com.xptlabs.varliktakibi.presentation.main.MainTabView
import com.xptlabs.varliktakibi.presentation.onboarding.OnboardingScreen
import com.xptlabs.varliktakibi.presentation.splash.SplashScreen
import com.xptlabs.varliktakibi.presentation.splash.SplashViewModel

@Composable
fun AssetTrackerNavHost(
    navController: NavHostController,
    adMobManager: AdMobManager,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val uiState by splashViewModel.uiState.collectAsState()

            SplashScreen(
                uiState = uiState,
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    // Onboarding tamamlandığında direkt ana sayfaya git
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainTabView(adMobManager = adMobManager)
        }

        // Analytics Screen
        composable(Screen.Analytics.route) {
            AnalyticsScreen(navController = navController)
        }
    }
}