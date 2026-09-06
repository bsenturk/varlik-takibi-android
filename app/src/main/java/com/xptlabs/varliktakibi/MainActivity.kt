package com.xptlabs.varliktakibi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xptlabs.varliktakibi.ads.AdMobManager
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.prefs.DarkModePreference
import com.xptlabs.varliktakibi.history.TimeMachine
import com.xptlabs.varliktakibi.market.MarketDataStore
import com.xptlabs.varliktakibi.push.PushRegistrar
import com.xptlabs.varliktakibi.ui.AppRoot
import com.xptlabs.varliktakibi.ui.theme.VarlikTakibiTheme
import com.xptlabs.varliktakibi.ui.theme.shouldUseDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var adMobManager: AdMobManager
    @Inject lateinit var purchaseManager: PurchaseManager
    @Inject lateinit var market: MarketDataStore
    @Inject lateinit var timeMachine: TimeMachine
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var pushRegistrar: PushRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Fiyat yenileme yalnızca ekran öndeyken dönsün; arka planda günlük
        // anlık görüntüyü SnapshotWorker yazıyor.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                market.startAutoRefresh(this)
                try {
                    // Eksik günleri doldur — çevrimdışıysa sessizce atlar.
                    timeMachine.reconstructAll()
                } finally {
                    // repeatOnLifecycle STOPPED'da iptal ederken döngüyü de durdur.
                }
            }
            market.stopAutoRefresh()
        }

        // Pro durumu değişince reklam yüzeylerini anında güncelle.
        lifecycleScope.launch {
            purchaseManager.isPro.collect { adMobManager.onProStatusChanged(it) }
        }

        setContent {
            val darkModePreference by prefs.darkMode
                .collectAsState(initial = DarkModePreference.SYSTEM)

            LaunchedEffect(Unit) {
                // İzin ayarlardan değiştirilmiş olabilir; her açılışta eşitle.
                pushRegistrar.sync()
                purchaseManager.refreshCustomerInfo()
            }

            VarlikTakibiTheme(darkTheme = shouldUseDarkTheme(darkModePreference)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        adMobManager = adMobManager,
                        purchaseManager = purchaseManager
                    )
                }
            }
        }
    }
}
