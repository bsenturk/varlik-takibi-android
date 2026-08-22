package com.xptlabs.varliktakibi.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.core.ext.toColor
import com.xptlabs.varliktakibi.core.format.TrFormat
import com.xptlabs.varliktakibi.data.local.entity.color
import com.xptlabs.varliktakibi.ui.common.PortfolioChip
import com.xptlabs.varliktakibi.ui.common.ValueChart
import com.xptlabs.varliktakibi.ui.theme.AppColors

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Analiz",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.portfolios, key = { it.id }) { portfolio ->
                PortfolioChip(
                    portfolio = portfolio,
                    isSelected = portfolio.id == state.selectedPortfolio?.id,
                    // Analiz sayfasında chip düzenleme açmıyor.
                    showsEditPencil = false,
                    onClick = { viewModel.selectPortfolio(portfolio) }
                )
            }
        }

        if (state.isEmpty) {
            EmptyState()
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ValueCard(state, viewModel) }
            item { RangePicker(selected = state.range, onSelect = viewModel::setRange) }
            item { DistributionCard(state) }
            if (state.gainers.isNotEmpty()) {
                item { MoversCard(title = "En Çok Yükselenler", items = state.gainers) }
            }
            if (state.losers.isNotEmpty()) {
                item { MoversCard(title = "En Çok Düşenler", items = state.losers) }
            }
        }
    }
}

@Composable
private fun ValueCard(state: AnalysisUiState, viewModel: AnalysisViewModel) {
    val changeColor = AppColors.forChange(state.rangeChangePercent)

    Card {
        Text(
            text = "Portföy Değeri",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (state.valuesMasked) TrFormat.MASK
            else TrFormat.money(
                viewModel.convert(state.metrics.totalValue, state.currency),
                state.currency
            ),
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${state.range.label} · ${TrFormat.percent(state.rangeChangePercent)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = changeColor
        )
        ValueChart(
            points = state.chart,
            lineColor = changeColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun RangePicker(selected: TimeRange, onSelect: (TimeRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.subtleFill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable { onSelect(range) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistributionCard(state: AnalysisUiState) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Dağılım",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.distribution.size} tür",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Yatay yığılmış oran çubuğu — pasta grafiğe göre dar ekranda okunaklı.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            state.distribution.forEach { slice ->
                Box(
                    modifier = Modifier
                        .weight(slice.percent.toFloat().coerceAtLeast(0.01f))
                        .fillMaxSize()
                        .background(slice.tintHex.toColor())
                )
            }
        }

        state.distribution.forEach { slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slice.tintHex.toColor())
                )
                Text(
                    text = slice.name,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TrFormat.slicePercent(slice.percent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MoversCard(title: String, items: List<MoverItem>) {
    Card {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(item.tintHex.toColor())
                )
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TrFormat.percent(item.changePercent),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.forChange(item.changePercent)
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = "Analiz için önce portföyüne varlık ekle.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
