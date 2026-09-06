package com.xptlabs.varliktakibi.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xptlabs.varliktakibi.data.local.entity.PortfolioEntity
import com.xptlabs.varliktakibi.data.local.entity.color
import com.xptlabs.varliktakibi.ui.common.AssetRow
import com.xptlabs.varliktakibi.ui.common.BalanceCard
import com.xptlabs.varliktakibi.ui.common.DeleteConfirmDialog
import com.xptlabs.varliktakibi.ui.common.PortfolioChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEditAsset: (String) -> Unit,
    onPortfolioLimitReached: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editorMode by remember { mutableStateOf<PortfolioEditorMode?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Header(
                onAddPortfolio = {
                    if (viewModel.canCreatePortfolio()) editorMode = PortfolioEditorMode.Create
                    else onPortfolioLimitReached()
                }
            )

            // Chip şeridi dikey listenin dışında: iç içe geçmiş kaydırmada
            // chip'lerin dokunma alanı bozuluyor.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.portfolios, key = { it.id }) { portfolio ->
                    PortfolioChip(
                        portfolio = portfolio,
                        isSelected = portfolio.id == state.selectedPortfolio?.id,
                        onClick = {
                            if (portfolio.id == state.selectedPortfolio?.id && !portfolio.isGeneral) {
                                editorMode = PortfolioEditorMode.Edit(portfolio)
                            } else {
                                viewModel.selectPortfolio(portfolio)
                            }
                        }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BalanceCard(
                            portfolioColor = state.selectedPortfolio?.color
                                ?: com.xptlabs.varliktakibi.core.model.PortfolioColor.BLUE,
                            metrics = state.metrics,
                            currency = state.currency,
                            onCurrencyChange = viewModel::setCurrency,
                            valuesMasked = state.valuesMasked,
                            onToggleMask = viewModel::toggleMask,
                            convert = { viewModel.convert(it, state.currency) }
                        )
                    }

                    if (state.isEmpty) {
                        item { EmptyState(isGeneral = state.isGeneralSelected) }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Varlıklarım",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = state.countLabel,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(state.rows, key = { it.id }) { row ->
                            AssetRow(
                                item = row,
                                currency = state.currency,
                                convert = { viewModel.convert(it, state.currency) },
                                valuesMasked = state.valuesMasked,
                                modifier = if (row.assetId != null) {
                                    Modifier.clickable { onEditAsset(row.assetId) }
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }
                }
            }
        }

        editorMode?.let { mode ->
            PortfolioEditorDialog(
                mode = mode,
                onSave = { name, color ->
                    when (mode) {
                        is PortfolioEditorMode.Create -> viewModel.createPortfolio(name, color)
                        is PortfolioEditorMode.Edit ->
                            viewModel.updatePortfolio(mode.portfolio, name, color)
                    }
                    editorMode = null
                },
                onDelete = {
                    (mode as? PortfolioEditorMode.Edit)?.let {
                        pendingDelete = it.portfolio.id to it.portfolio.name
                    }
                    editorMode = null
                },
                onDismiss = { editorMode = null }
            )
        }

        pendingDelete?.let { (id, name) ->
            DeleteConfirmDialog(
                title = "\"$name\" silinsin mi?",
                message = "Portföy ve içindeki tüm varlıklar kalıcı olarak silinecek.",
                onConfirm = {
                    state.portfolios.firstOrNull { it.id == id }
                        ?.let(viewModel::deletePortfolio)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
private fun Header(onAddPortfolio: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Portföylerim",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onAddPortfolio),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Portföy ekle",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(isGeneral: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Text(
            text = if (isGeneral) "Hadi başlayalım" else "Bu portföy boş",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isGeneral)
                "Aşağıdaki + butonuna dokun, ilk varlığını ekle ve portföyünün canlı değerini gör."
            else
                "Aşağıdaki + butonuna dokunarak bu portföye varlık ekle.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 30.dp)
        )
    }
}

sealed interface PortfolioEditorMode {
    data object Create : PortfolioEditorMode
    data class Edit(val portfolio: PortfolioEntity) : PortfolioEditorMode
}
