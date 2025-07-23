package com.xptlabs.varliktakibi.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.ads.components.AdMobBannerAd
import com.xptlabs.varliktakibi.presentation.analytics.AnalyticsScreen
import com.xptlabs.varliktakibi.presentation.assets.AssetsScreen
import com.xptlabs.varliktakibi.presentation.rates.RatesScreen
import com.xptlabs.varliktakibi.presentation.settings.SettingsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Assets : BottomNavItem("assets", "Varlıklarım", Icons.Default.AccountBalanceWallet)
    object Rates : BottomNavItem("rates", "Kurlar", Icons.Default.TrendingUp)
    object Settings : BottomNavItem("settings", "Ayarlar", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabView(
    adMobManager: AdMobManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Assets,
        BottomNavItem.Rates,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            BottomNavigation(
                navController = navController,
                items = items
            )
        }
    ) { paddingValues ->
        // Ana content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content (takes remaining space)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = BottomNavItem.Assets.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(BottomNavItem.Assets.route) {
                        // Assets screen ile banner ad'ı birlikte göster
                        AssetsScreenWithBanner(
                            navController = navController,
                            adMobManager = adMobManager
                        )
                    }
                    composable(BottomNavItem.Rates.route) {
                        RatesScreen()
                    }
                    composable(BottomNavItem.Settings.route) {
                        SettingsScreen()
                    }
                    // Analytics screen - accessible from Assets screen
                    composable("analytics") {
                        AnalyticsScreen(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsScreenWithBanner(
    navController: NavController,
    adMobManager: AdMobManager
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AssetsScreen(
            navController = navController,
            modifier = Modifier.weight(1f)
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        /*AdMobBannerAd(
            adMobManager = adMobManager,
            modifier = Modifier.fillMaxWidth(),
            screenName = "assets_screen",
            onAdLoaded = {
                // Banner ad loaded successfully on Assets screen
            },
            onAdFailedToLoad = { error ->
                // Handle ad load failure on Assets screen
            },
            onAdClicked = {
                // Handle ad click on Assets screen
            }
        )*/
    }
}

@Composable
private fun BottomNavigation(
    navController: NavController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination to avoid building up a large stack
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}