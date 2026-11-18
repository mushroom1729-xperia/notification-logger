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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mushroom.notificationlogger.service.ListenerConnectionUiState

@Composable
fun PermissionBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionGuidanceBanner(
        title = "通知へのアクセスが未許可です",
        body = "通知を記録するには、Android 設定の「通知へのアクセス」で Notification Logger を有効にしてください。記録は端末内のみに保存され、外部には一切送信されません。",
        primaryActionLabel = "通知アクセス設定を開く",
        onPrimaryAction = onOpenSettings,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
    )
}

@Composable
fun BatteryOptimizationBanner(
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionGuidanceBanner(
        title = "バッテリー最適化の対象になっています",
        body = "スリープ中やバックグラウンドで通知の記録が停止されるのを防ぐため、バッテリー最適化を「対象外（制限なし）」に設定してください。",
        primaryActionLabel = "バッテリー最適化を解除",
        onPrimaryAction = onRequestIgnoreBatteryOptimizations,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    )
}

@Composable
fun BackgroundRestrictionBanner(
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionGuidanceBanner(
        title = "バックグラウンド動作が制限されています",
        body = "システムのバッテリー設定でバックグラウンド制限が有効になっているため、通知を取りこぼすおそれがあります。アプリ情報画面から制限を解除してください。",
        primaryActionLabel = "アプリ情報を開く",
        onPrimaryAction = onOpenAppInfo,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
    )
}

@Composable
fun ListenerConnectionStatusSection(
    connectionState: ListenerConnectionUiState,
    onRetryRebind: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (connectionState) {
        ListenerConnectionUiState.CONNECTED -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "通知リスナー接続中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ListenerConnectionUiState.DISCONNECTED_NEEDS_REBIND -> {
            ActionGuidanceBanner(
                title = "通知リスナーが切断されています",
                body = "通知アクセスは許可されていますが、システムとの接続が切断されています。再接続を試行するか、改善しない場合は通知アクセス設定を切り替え直してください。",
                primaryActionLabel = "再接続を試行",
                onPrimaryAction = onRetryRebind,
                secondaryActionLabel = "通知アクセス設定",
                onSecondaryAction = onOpenNotificationSettings,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = modifier
            )
        }

        ListenerConnectionUiState.DISCONNECTED_NO_PERMISSION -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "通知リスナー切断中（通知へのアクセス未許可）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ActionGuidanceBanner(
    title: String,
    body: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        onClick = onPrimaryAction,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(secondaryActionLabel)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Button(onClick = onPrimaryAction) {
                    Text(primaryActionLabel)
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
