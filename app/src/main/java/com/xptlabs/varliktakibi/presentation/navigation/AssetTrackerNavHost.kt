package com.xptlabs.varliktakibi.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xptlabs.varliktakibi.presentation.permissions.PermissionScreen
import com.xptlabs.varliktakibi.presentation.splash.SplashScreen
import com.xptlabs.varliktakibi.presentation.splash.SplashViewModel
import com.xptlabs.varliktakibi.presentation.test.TestIconsScreen

@Composable
fun AssetTrackerNavHost(
    navController: NavHostController,
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
                    navController.navigate("permissions") {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable("permissions") {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo("permissions") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            androidx.compose.material3.Text("Onboarding Screen - Coming Soon!")
        }

        composable(Screen.Main.route) {
            TestIconsScreen()
        }
    }
}