package com.xptlabs.varliktakibi.presentation.test

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.xptlabs.varliktakibi.managers.AssetTrackerWorkManager
import com.xptlabs.varliktakibi.presentation.components.GradientButton
import com.xptlabs.varliktakibi.presentation.components.IconWithBackground
import com.xptlabs.varliktakibi.workers.AssetTrackerSyncWorker

@Composable
fun TestWorkManagerScreen(
    workManager: AssetTrackerWorkManager
) {
    var workStatus by remember { mutableStateOf("Idle") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IconWithBackground(
            icon = Icons.Default.Work,
            contentDescription = "Work Manager",
            size = 80.dp,
            iconSize = 40.dp
        )

        Text(
            text = "WorkManager Test",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Background Work Status:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = workStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GradientButton(
                text = "Start One-Time Sync",
                onClick = {
                    workManager.startOneTimeSyncNow()
                    workStatus = "One-time sync started..."
                }
            )

            OutlinedButton(
                onClick = {
                    workManager.startPeriodicSync()
                    workStatus = "Periodic sync started (every 15 min)"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Periodic Sync")
            }

            OutlinedButton(
                onClick = {
                    workManager.stopAllWork()
                    workStatus = "All background work stopped"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop All Work")
            }
        }

        // Work Info Monitor
        LaunchedEffect(Unit) {
            // Monitor work status
            // This is a simplified version - in real app you'd use LiveData/Flow
        }
    }
}