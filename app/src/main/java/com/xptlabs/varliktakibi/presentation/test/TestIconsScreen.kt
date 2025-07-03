package com.xptlabs.varliktakibi.presentation.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground

@Composable
fun TestIconsScreen() {
    val icons = listOf(
        Icons.Default.AccountBalanceWallet to "Wallet",
        Icons.Default.TrendingUp to "Trending Up",
        Icons.Default.CurrencyExchange to "Currency Exchange",
        Icons.Default.Savings to "Savings",
        Icons.Default.MonetizationOn to "Money",
        Icons.Default.Analytics to "Analytics",
        Icons.Default.Settings to "Settings",
        Icons.Default.Notifications to "Notifications",
        Icons.Default.Add to "Add",
        Icons.Default.Home to "Home"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Material Icons Extended Test",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            GradientButton(
                text = "Test Gradient Button",
                onClick = { /* Test */ }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Available Icons:",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(icons.chunked(3)) { iconRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                iconRow.forEach { (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconWithBackground(
                            icon = icon,
                            contentDescription = label,
                            size = 56.dp,
                            iconSize = 28.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Fill empty spaces if row is not complete
                repeat(3 - iconRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TestIconsScreenPreview() {
    TestIconsScreen()
}