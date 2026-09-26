package com.xptlabs.varliktakibi

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import com.xptlabs.varliktakibi.widget.PortfolioWidget
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

    /** Kilitli widget'a dokunulup açıldıysa paywall gösterilecek. */
    private val widgetPaywallRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            widgetPaywallRequest.value = intent.getBooleanExtra(EXTRA_OPEN_PAYWALL, false)
        }

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

        // Önce reklam onayı (AEA/İngiltere), sonra reklam SDK'sı. Pro'da ikisi de yok.
        if (savedInstanceState == null) {
            lifecycleScope.launch { adMobManager.start(this@MainActivity) }
        }

        // Pro durumu değişince reklam yüzeylerini anında güncelle.
        lifecycleScope.launch {
            purchaseManager.isPro.collect {
                adMobManager.onProStatusChanged(it)
                // Abonelik bitince kullanıcı uygulamayı açmasa da widget kilide düşsün.
                PortfolioWidget.refresh(applicationContext)
            }
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
                        purchaseManager = purchaseManager,
                        widgetPaywallRequested = widgetPaywallRequest.value,
                        onWidgetPaywallConsumed = { widgetPaywallRequest.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PAYWALL, false)) widgetPaywallRequest.value = true
    }

    /** Uygulamadan çıkarken widget son değişiklikleri (varlık, portföy, göz) alsın. */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { PortfolioWidget.refresh(applicationContext) }
    }

    companion object {
        const val EXTRA_OPEN_PAYWALL = "open_paywall"
    }
}
