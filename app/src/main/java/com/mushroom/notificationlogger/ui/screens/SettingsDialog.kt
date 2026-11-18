package com.mushroom.notificationlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.ui.components.AppIcons
import com.mushroom.notificationlogger.ui.components.MushroomBadgeIcon

@Composable
fun SettingsDialog(
    themeMode: ThemeMode,
    excludeOngoing: Boolean,
    isAccessGranted: Boolean,
    isServiceConnected: Boolean,
    totalCount: Int,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleExcludeOngoing: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestDeleteAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { MushroomBadgeIcon(size = 32.dp) },
        title = { Text("設定・ステータス") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // テーマ切替 (システム連動 / ライト / ダーク)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "外観テーマ",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val options = listOf(
                        ThemeMode.SYSTEM to "システム",
                        ThemeMode.LIGHT to "ライト",
                        ThemeMode.DARK to "ダーク"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                label = { Text(label) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 常駐・進行中通知の除外設定
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "常駐・進行中通知を除外",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "音楽再生やバックグラウンド常駐通知を記録しません（グループまとめ通知は常に除外されます）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = excludeOngoing,
                        onCheckedChange = onToggleExcludeOngoing
                    )
                }

                HorizontalDivider()

                // サービス接続状態と権限設定導線
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "通知取得サービス状態",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val statusText = when {
                        !isAccessGranted -> "権限が未許可です"
                        isServiceConnected -> "正常に稼働中（接続済み）"
                        else -> "権限許可済み（システム接続待機中）"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAccessGranted) AppIcons.CheckCircle else AppIcons.Warning,
                            contentDescription = null,
                            tint = if (isAccessGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Android の通知アクセス設定を開く")
                    }
                }

                if (totalCount > 0) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onRequestDeleteAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("すべての記録を削除 ($totalCount 件)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
