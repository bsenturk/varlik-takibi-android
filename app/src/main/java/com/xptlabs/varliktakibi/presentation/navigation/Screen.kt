package com.xptlabs.varliktakibi.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")

    // Main Tab Screens
    object Assets : Screen("assets")
    object Rates : Screen("rates")
    object Settings : Screen("settings")

    // Detail Screens
    object AssetDetail : Screen("asset_detail/{assetId}") {
        fun createRoute(assetId: String) = "asset_detail/$assetId"
    }
    object Analytics : Screen("analytics")
}