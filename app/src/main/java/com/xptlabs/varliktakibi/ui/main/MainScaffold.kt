package com.xptlabs.varliktakibi.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.core.ext.toColor

enum class MainTab(val title: String, val icon: ImageVector) {
    PORTFOLIO("Portföy", Icons.Filled.CreditCard),

    // Piyasalar'ın çizgi grafiğine benzemesin diye pasta grafik.
    ANALYSIS("Analiz", Icons.Filled.PieChart),
    RATES("Piyasalar", Icons.AutoMirrored.Filled.ShowChart),
    SETTINGS("Ayarlar", Icons.Filled.Settings)
}

/**
 * Yüzen kapsül tab bar + ortada gradyanlı ekleme düğmesi. iOS `MainTabView`
 * yerleşiminin portu: hap içeriğin üzerinde yüzer, altında banner reklam.
 */
@Composable
fun MainScaffold(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddAsset: () -> Unit,
    showBanner: Boolean,
    banner: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) { content() }

        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            banner()
        }

        CapsuleTabBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onAddAsset = onAddAsset
        )
    }
}

@Composable
private fun CapsuleTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddAsset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // FAB kapsülün üstüne taştığı için üstte pay bırakılıyor; Box
            // varsayılan olarak kırpmadığından taşan kısım görünür kalıyor.
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(percent = 50))
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(percent = 50)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f)) {
                TabButton(MainTab.PORTFOLIO, selectedTab, onTabSelected, Modifier.weight(1f))
                TabButton(MainTab.ANALYSIS, selectedTab, onTabSelected, Modifier.weight(1f))
            }

            // Ortadaki FAB'ın oturacağı boşluk.
            Box(modifier = Modifier.width(76.dp))

            Row(modifier = Modifier.weight(1f)) {
                TabButton(MainTab.RATES, selectedTab, onTabSelected, Modifier.weight(1f))
                TabButton(MainTab.SETTINGS, selectedTab, onTabSelected, Modifier.weight(1f))
            }
        }

        AddButton(
            onClick = onAddAsset,
            modifier = Modifier.offset(y = (-18).dp)
        )
    }
}

@Composable
private fun TabButton(
    tab: MainTab,
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = tab == selectedTab
    val tint = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onTabSelected(tab) }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(text = tab.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf("#0A84FF".toColor(), "#AF52DE".toColor())
                )
            )
            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Varlık ekle",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
