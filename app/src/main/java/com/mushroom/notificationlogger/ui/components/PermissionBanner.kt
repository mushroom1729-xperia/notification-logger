package com.mushroom.notificationlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "通知へのアクセスが未許可です",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "通知を記録するには、Android 設定の「通知へのアクセス」で Notification Logger を有効にしてください。記録は端末内のみに保存され、外部には一切送信されません。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onOpenSettings) {
                    Text("通知アクセス設定を開く")
                }
            }
        }
    }
}

@Composable
fun FirstLaunchPermissionDialog(
    onConfirmOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { MushroomBadgeIcon(size = 36.dp) },
        title = {
            Text("Notification Logger の初期設定")
        },
        text = {
            Text(
                "端末に届く全アプリの通知をローカルに記録するため、システム設定から「通知へのアクセス」を許可してください。\n\n" +
                    "・ネットワーク通信は一切行いません\n" +
                    "・すべてのデータは端末内の専用領域に保存されます"
            )
        },
        confirmButton = {
            Button(onClick = onConfirmOpenSettings) {
                Text("設定画面を開く")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("あとで")
            }
        }
    )
}
