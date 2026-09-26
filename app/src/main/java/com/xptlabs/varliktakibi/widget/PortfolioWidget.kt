package com.xptlabs.varliktakibi.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.xptlabs.varliktakibi.MainActivity
import com.xptlabs.varliktakibi.billing.PurchaseManager
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.core.model.PortfolioColor
import com.xptlabs.varliktakibi.data.local.entity.assetType
import com.xptlabs.varliktakibi.data.prefs.AppPreferences
import com.xptlabs.varliktakibi.data.remote.MarketDataService
import com.xptlabs.varliktakibi.data.repo.PortfolioRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Uygulamadaki bakiye kartının ana ekran hâli (Pro). iOS `PortfolioWidget`
 * karşılığı.
 *
 * iOS'ta widget ayrı süreçte çalıştığı için uygulama App Group'a varlık listesi
 * yazıyor; Android'de widget uygulamanın kendi sürecinde, Room'u ve tercihleri
 * doğrudan okuyor. Fiyatları kendisi çekiyor (yalnızca tutulan semboller), yani
 * kullanıcı uygulamayı açmasa da 30 dakikada bir tazeleniyor.
 */
class PortfolioWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = runCatching { loadState(context) }
            .onFailure { Log.w(TAG, "Widget verisi okunamadı: ${it.message}") }
            .getOrDefault(WidgetState.NeedsApp)
        provideContent { WidgetContent(context, state) }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val deps = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val prefs = deps.prefs()
        val isPro = deps.purchaseManager().cachedIsPro()
        val portfolios = deps.repository().portfolios()
        val assets = deps.repository().assets()
        val currency = prefs.selectedCurrency.first()

        // Kilitliyken fiyat çekilmiyor: tutarlar zaten maskeli.
        val prices = if (!isPro) emptyList() else runCatching {
            val symbols = assets.filter { !it.assetType.isManual }.map { it.symbol }.toSet() +
                setOf("USD", currency.code)
            deps.marketDataService().fetchPrices(symbols)
        }.onFailure { Log.w(TAG, "Widget fiyatları çekilemedi: ${it.message}") }.getOrNull()

        return buildWidgetState(
            portfolios = portfolios,
            assets = assets,
            selectedPortfolioId = prefs.selectedPortfolioId.first(),
            maskedPortfolioIds = prefs.maskedPortfolioIds.first(),
            currency = currency,
            isPro = isPro,
            prices = prices
        )
    }

    companion object {
        private const val TAG = "PortfolioWidget"
        private val SMALL = DpSize(150.dp, 110.dp)
        private val MEDIUM = DpSize(260.dp, 110.dp)

        /** Uygulamada veri ya da Pro durumu değişince çağrılır; hata yutulur. */
        suspend fun refresh(context: Context) {
            runCatching { PortfolioWidget().updateAll(context) }
                .onFailure { Log.w(TAG, "Widget yenilenemedi: ${it.message}") }
        }
    }
}

class PortfolioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PortfolioWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): PortfolioRepository
    fun prefs(): AppPreferences
    fun purchaseManager(): PurchaseManager
    fun marketDataService(): MarketDataService
}

// ── Arayüz ───────────────────────────────────────────────────────────────────

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.85f))
private val WhiteFaint = ColorProvider(Color.White.copy(alpha = 0.7f))

@Composable
private fun WidgetContent(context: Context, state: WidgetState) {
    val card = when (state) {
        is WidgetState.Ready -> state.card
        is WidgetState.Locked -> state.card
        WidgetState.NeedsApp -> null
    }
    // Kilitli kart doğrudan paywall'ı açar; kullanıcıyı uygulamada "nereye
    // basacaktım" diye aramaya bırakmıyor.
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .apply { if (state is WidgetState.Locked) putExtra(MainActivity.EXTRA_OPEN_PAYWALL, true) }

    // Kilitli kart da portföyün kendi degradesini kullanır; veri yokken nötr gri.
    val colors = card?.let { PortfolioColor.fromHex(it.colorHex).gradient }
        ?: listOf("#383838".toColor(), "#212121".toColor())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .background(ImageProvider(gradientBitmap(colors)), ContentScale.FillBounds)
            .clickable(actionStartActivity(intent))
            .padding(14.dp)
    ) {
        when {
            card != null -> CardBody(card, medium = LocalSize.current.width >= 250.dp)
            state is WidgetState.Locked -> Message("🔒", "Varlık Pro", "Portföy widget'ı Pro üyelere açık. Açmak için dokun.")
            else -> Message("📲", null, "Portföyünü görmek için uygulamayı bir kez aç.")
        }
    }
}

@Composable
private fun CardBody(card: WidgetCard, medium: Boolean) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        // Dikey boşluklar (defaultWeight) ancak kolon tam yükseklikteyse dağılıyor.
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Text(
                text = card.name,
                style = TextStyle(color = WhiteSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = card.total,
                style = TextStyle(color = White, fontSize = if (medium) 24.sp else 20.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(6.dp))
            when {
                card.isLocked -> Pill("🔒 Pro ile aç")
                card.hasProfitLoss -> Pill(
                    (if (card.isPositive) "↑ " else "↓ ") + "%" + TrFormat.decimal(abs(card.percent)) +
                        if (medium) " · ${card.profitLoss}" else ""
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            // Fiyat çekilemedi: tutarın bayat olduğunu söyle, sessizce eskiyi gösterme.
            if (card.stale) {
                Text("Fiyatlar güncellenemedi", style = TextStyle(color = WhiteFaint, fontSize = 9.sp), maxLines = 1)
            }
        }
        if (medium && card.rows.isNotEmpty()) {
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.width(130.dp)) {
                card.rows.forEach { row ->
                    HoldingRow(row, showPercent = !card.masked)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HoldingRow(row: WidgetCard.Row, showPercent: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(ColorProvider(row.tintHex.toColor()))
        ) {}
        Spacer(GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(row.name, style = TextStyle(color = White, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(row.value, style = TextStyle(color = WhiteFaint, fontSize = 10.sp), maxLines = 1)
        }
        if (showPercent) {
            Text(
                "%" + TrFormat.decimal(abs(row.percent)),
                style = TextStyle(color = if (row.percent >= 0) White else WhiteFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(ColorProvider(Color.White.copy(alpha = 0.2f)))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = TextStyle(color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun Message(icon: String, title: String?, text: String) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(icon, style = TextStyle(fontSize = 18.sp))
        Spacer(GlanceModifier.defaultWeight())
        if (title != null) {
            Text(title, style = TextStyle(color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
        Text(text, style = TextStyle(color = WhiteSoft, fontSize = 11.sp))
    }
}

/** Glance degrade desteklemiyor; kart zemini küçük bir bitmap olarak çiziliyor. */
private fun gradientBitmap(colors: List<Color>): Bitmap {
    val w = 300
    val h = 150
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors.first().toArgb(), colors.last().toArgb(), Shader.TileMode.CLAMP
        )
    }
    Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    return bitmap
}
