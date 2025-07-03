package com.xptlabs.varliktakibi.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xptlabs.varliktakibi.utils.PermissionHelper

@Composable
fun NotificationPermissionDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Bildirim İzni",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Varlık takibi güncellemeleri ve önemli bilgilendirmeler için bildirim iznine ihtiyacımız var.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "• Güncel kur bilgilendirmeleri\n• Portföy değer değişiklikleri\n• Önemli piyasa haberleri",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            GradientButton(
                text = "İzin Ver",
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Şimdi Değil")
            }
        },
        modifier = modifier
    )
}

@Composable
fun PermissionDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Bildirim İzni Gerekli",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Bildirimleri etkinleştirmek için uygulama ayarlarından bildirim iznini manuel olarak açmanız gerekiyor.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ayarlara Git")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("İptal")
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
fun NotificationPermissionDialogPreview() {
    MaterialTheme {
        NotificationPermissionDialog(
            onRequestPermission = {},
            onDismiss = {}
        )
    }
}