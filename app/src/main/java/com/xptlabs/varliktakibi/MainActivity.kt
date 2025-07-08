package com.xptlabs.varliktakibi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.xptlabs.varliktakibi.ui.theme.AssetTrackerTheme
import com.xptlabs.varliktakibi.ui.theme.ThemeViewModel
import com.xptlabs.varliktakibi.ui.theme.shouldUseDarkTheme
import com.xptlabs.varliktakibi.presentation.navigation.AssetTrackerNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val darkModePreference by themeViewModel.darkModePreference.collectAsState()
            val useDarkTheme = shouldUseDarkTheme(darkModePreference)

            AssetTrackerTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false // Keep consistent branding
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AssetTrackerNavHost(navController = navController)
                }
            }
        }
    }
}