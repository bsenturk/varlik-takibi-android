package com.xptlabs.varliktakibi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.notifications.AppNotificationManager
import com.xptlabs.varliktakibi.presentation.navigation.AssetTrackerNavHost
import com.xptlabs.varliktakibi.ui.theme.AssetTrackerTheme
import com.xptlabs.varliktakibi.ui.theme.ThemeViewModel
import com.xptlabs.varliktakibi.ui.theme.shouldUseDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var notificationManager: AppNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if opened from notification
        handleNotificationIntent()

        Log.d("MainActivity", "onCreate - App Open Ad Unit ID: ${adMobManager.getAppOpenAdUnitId()}")
        Log.d("MainActivity", "onCreate - Banner Ad Unit ID: ${adMobManager.getBannerAdUnitId()}")

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val darkModePreference by themeViewModel.darkModePreference.collectAsState()
            val useDarkTheme = shouldUseDarkTheme(darkModePreference)

            // App lifecycle actions
            LaunchedEffect(Unit) {
                Log.d("MainActivity", "LaunchedEffect - Waiting for AdMob initialization")

                // Wait for AdMob to initialize
                var attempts = 0
                while (!adMobManager.isAdMobReady() && attempts < 30) {
                    delay(100)
                    attempts++
                }

                if (adMobManager.isAdMobReady()) {
                    Log.d("MainActivity", "AdMob ready, showing app open ad")
                    delay(500) // Small delay to ensure UI is ready
                    adMobManager.showAppOpenAd(this@MainActivity)
                } else {
                    Log.e("MainActivity", "AdMob initialization timeout")
                }

                // Schedule next notification on each app start
                notificationManager.scheduleNextNotification()
                Log.d("MainActivity", "Next notification scheduled")
            }

            AssetTrackerTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false // Keep consistent branding
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AssetTrackerNavHost(
                        navController = navController,
                        adMobManager = adMobManager
                    )
                }
            }
        }
    }

    private fun handleNotificationIntent() {
        val fromNotification = intent.getBooleanExtra("from_notification", false)
        val messageIndex = intent.getIntExtra("notification_message_index", -1)

        if (fromNotification) {
            Log.d("MainActivity", "Opened from notification, message index: $messageIndex")

            // Analytics for notification clicks
            // You can add navigation to specific screen here if needed
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume - AdMob ready: ${adMobManager.isAdMobReady()}")
        Log.d("MainActivity", "onResume - Notifications enabled: ${notificationManager.areNotificationsEnabled()}")
    }
}