package com.mushroom.notificationlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mushroom.notificationlogger.data.preferences.HealthCheckRecord
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.service.BackgroundRestrictionState
import com.mushroom.notificationlogger.service.BatteryOptimizationState
import com.mushroom.notificationlogger.ui.components.AppIcons
import com.mushroom.notificationlogger.ui.components.MushroomBadgeIcon
import com.mushroom.notificationlogger.ui.util.DateTimeFormatterUtil

@Composable
fun SettingsDialog(
    themeMode: ThemeMode,
    excludeOngoing: Boolean,
    isAccessGranted: Boolean,
    isServiceConnected: Boolean,
    batteryOptimizationState: BatteryOptimizationState,
    backgroundRestrictionState: BackgroundRestrictionState,
    healthCheckRecord: HealthCheckRecord?,
    totalCount: Int,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleExcludeOngoing: (Boolean) -> Unit,
    onRequestRebind: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestDeleteAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { MushroomBadgeIcon(size = 32.dp) },
        title = { Text("設定・ステータス") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                    val isHealthyConnection = isAccessGranted && isServiceConnected
                    val statusText = when {
                        !isAccessGranted -> "未接続（通知へのアクセス未許可）"
                        isServiceConnected -> "正常に稼働中（接続済み）"
                        else -> "切断中（再バインドが必要）"
                    }
                    StatusIndicatorRow(
                        isHealthy = isHealthyConnection,
                        text = statusText
                    )

                    val checkCaption = remember(healthCheckRecord) {
                        if (healthCheckRecord != null) {
                            val formatted = DateTimeFormatterUtil.formatFullTimestamp(
                                healthCheckRecord.checkedAtMillis
                            )
                            "定期点検（約6時間間隔）: 最終確認 $formatted"
                        } else {
                            "定期点検: 約6時間間隔で接続・最適化状態を自動確認"
                        }
                    }
                    Text(
                        text = checkCaption,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAccessGranted && !isServiceConnected) {
                        OutlinedButton(
                            onClick = onRequestRebind,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("リスナーの再接続を試行")
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Android の通知アクセス設定を開く")
                    }
                }

                HorizontalDivider()

                // バッテリー最適化・バックグラウンド制限の状態と設定導線
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "バッテリー・バックグラウンド動作",
                        style = MaterialTheme.typography.titleMedium
                    )

                    StatusIndicatorRow(
                        isHealthy = batteryOptimizationState.isIgnoringOptimizations,
                        text = "バッテリー最適化: ${batteryOptimizationState.settingsStatusText}"
                    )

                    if (batteryOptimizationState.shouldShowBanner) {
                        OutlinedButton(
                            onClick = onRequestIgnoreBatteryOptimizations,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("バッテリー最適化の対象外にする")
                        }
                    }

                    StatusIndicatorRow(
                        isHealthy = !backgroundRestrictionState.shouldWarn,
                        text = "バックグラウンド制限: ${backgroundRestrictionState.settingsStatusText}"
                    )

                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        colors = if (backgroundRestrictionState.shouldWarn) {
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("アプリ情報画面を開く")
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

@Composable
private fun StatusIndicatorRow(
    isHealthy: Boolean,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isHealthy) AppIcons.CheckCircle else AppIcons.Warning,
            contentDescription = null,
            tint = if (isHealthy) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHealthy) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}
