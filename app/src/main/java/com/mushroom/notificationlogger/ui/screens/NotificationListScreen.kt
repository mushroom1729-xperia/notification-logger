package com.mushroom.notificationlogger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushroom.notificationlogger.data.local.AppSummary
import com.mushroom.notificationlogger.data.local.NotificationEntity
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import com.mushroom.notificationlogger.service.BackgroundExecutionHelper
import com.mushroom.notificationlogger.service.BackgroundRestrictionState
import com.mushroom.notificationlogger.service.BatteryOptimizationState
import com.mushroom.notificationlogger.service.NotificationAccessHelper
import com.mushroom.notificationlogger.service.NotificationCaptureService
import com.mushroom.notificationlogger.ui.MainViewModel
import com.mushroom.notificationlogger.ui.components.AppIconImage
import com.mushroom.notificationlogger.ui.components.AppIcons
import com.mushroom.notificationlogger.ui.components.BackgroundRestrictionBanner
import com.mushroom.notificationlogger.ui.components.BatteryOptimizationBanner
import com.mushroom.notificationlogger.ui.components.FirstLaunchPermissionDialog
import com.mushroom.notificationlogger.ui.components.ListenerConnectionStatusSection
import com.mushroom.notificationlogger.ui.components.MushroomBadgeIcon
import com.mushroom.notificationlogger.ui.components.PermissionBanner
import com.mushroom.notificationlogger.ui.components.SavedNotificationThumbnail
import com.mushroom.notificationlogger.ui.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    viewModel: MainViewModel,
    isNotificationAccessGranted: Boolean,
    batteryOptimizationState: BatteryOptimizationState,
    backgroundRestrictionState: BackgroundRestrictionState,
    onOpenNotificationDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val recordedApps by viewModel.recordedApps.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPackageName by viewModel.selectedPackageName.collectAsStateWithLifecycle()
    val isLoadingList by viewModel.isLoadingList.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val excludeOngoing by viewModel.excludeOngoing.collectAsStateWithLifecycle()
    val healthCheckRecord by viewModel.healthCheckRecord.collectAsStateWithLifecycle()
    val isServiceConnected by NotificationCaptureService.isServiceConnected.collectAsStateWithLifecycle()

    val listenerConnectionState = remember(isNotificationAccessGranted, isServiceConnected) {
        BackgroundExecutionHelper.resolveListenerConnectionState(
            isAccessGranted = isNotificationAccessGranted,
            isServiceConnected = isServiceConnected
        )
    }

    var hasPromptedPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showExportScopeDialog by rememberSaveable { mutableStateOf(false) }
    var showAppPickerSheet by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<NotificationEntity?>(null) }

    val isFilterActive = selectedPackageName != null || searchQuery.isNotBlank()

    fun triggerRebind() {
        val requested = NotificationAccessHelper.ensureListenerBound(context)
        coroutineScope.launch {
            val message = if (requested) {
                "通知リスナーの再接続を要求しました"
            } else {
                "通知へのアクセスを先に許可してください"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    fun triggerExport(onlyFiltered: Boolean) {
        viewModel.exportJson(
            context = context,
            onlyFiltered = onlyFiltered,
            onIntentReady = { chooserIntent ->
                runCatching { context.startActivity(chooserIntent) }
            },
            onMessage = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MushroomBadgeIcon(size = 26.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notification Logger",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "記録数: $totalCount 件",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (totalCount == 0) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("エクスポートできる通知記録がありません")
                                }
                            } else if (isFilterActive) {
                                showExportScopeDialog = true
                            } else {
                                triggerExport(onlyFiltered = false)
                            }
                        },
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = AppIcons.Share,
                                contentDescription = "JSON エクスポート"
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = totalCount > 0
                    ) {
                        Icon(
                            imageVector = AppIcons.DeleteSweep,
                            contentDescription = "全件削除"
                        )
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = AppIcons.Settings,
                            contentDescription = "設定・テーマ切替"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ListenerConnectionStatusSection(
                connectionState = listenerConnectionState,
                onRetryRebind = ::triggerRebind,
                onOpenNotificationSettings = {
                    NotificationAccessHelper.openNotificationAccessSettings(context)
                }
            )

            if (!isNotificationAccessGranted) {
                PermissionBanner(
                    onOpenSettings = {
                        NotificationAccessHelper.openNotificationAccessSettings(context)
                    }
                )
            }

            if (backgroundRestrictionState.shouldWarn) {
                BackgroundRestrictionBanner(
                    onOpenAppInfo = {
                        BackgroundExecutionHelper.openAppDetailsSettings(context)
                    }
                )
            }

            if (batteryOptimizationState.shouldShowBanner) {
                BatteryOptimizationBanner(
                    onRequestIgnoreBatteryOptimizations = {
                        BackgroundExecutionHelper.requestIgnoreBatteryOptimizations(context)
                    }
                )
            }

            // キーワード検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text("キーワード検索（タイトル・本文・アプリ名）")
                },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = AppIcons.Clear,
                                contentDescription = "検索クリア"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // アプリ単位の絞り込みチップ行
            if (recordedApps.isNotEmpty() || selectedPackageName != null) {
                AppFilterChipRow(
                    recordedApps = recordedApps,
                    totalCount = totalCount,
                    selectedPackageName = selectedPackageName,
                    onSelectPackage = viewModel::onSelectPackage,
                    onOpenAppSheet = { showAppPickerSheet = true }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 一覧表示
            when {
                isLoadingList -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                notifications.isEmpty() -> {
                    EmptyNotificationsPlaceholder(
                        isFilterActive = isFilterActive,
                        isAccessGranted = isNotificationAccessGranted,
                        onClearFilters = viewModel::clearFilters,
                        onOpenSettings = {
                            NotificationAccessHelper.openNotificationAccessSettings(context)
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.notification.id }
                        ) { item ->
                            NotificationListCard(
                                item = item,
                                resolveImageFile = viewModel::resolveImageFile,
                                onClick = { onOpenNotificationDetail(item.notification.id) },
                                onRequestDelete = { pendingDeleteItem = item.notification }
                            )
                        }
                    }
                }
            }
        }
    }

    // 初回起動かつ未許可時の案内ダイアログ
    if (!isNotificationAccessGranted && !hasPromptedPermissionDialog) {
        FirstLaunchPermissionDialog(
            onConfirmOpenSettings = {
                hasPromptedPermissionDialog = true
                NotificationAccessHelper.openNotificationAccessSettings(context)
            },
            onDismiss = {
                hasPromptedPermissionDialog = true
            }
        )
    }

    // 設定・テーマ切替ダイアログ
    if (showSettingsDialog) {
        SettingsDialog(
            themeMode = themeMode,
            excludeOngoing = excludeOngoing,
            isAccessGranted = isNotificationAccessGranted,
            isServiceConnected = isServiceConnected,
            batteryOptimizationState = batteryOptimizationState,
            backgroundRestrictionState = backgroundRestrictionState,
            healthCheckRecord = healthCheckRecord,
            totalCount = totalCount,
            onSelectThemeMode = viewModel::setThemeMode,
            onToggleExcludeOngoing = viewModel::setExcludeOngoing,
            onRequestRebind = ::triggerRebind,
            onOpenNotificationSettings = {
                NotificationAccessHelper.openNotificationAccessSettings(context)
            },
            onRequestIgnoreBatteryOptimizations = {
                BackgroundExecutionHelper.requestIgnoreBatteryOptimizations(context)
            },
            onOpenAppInfo = {
                BackgroundExecutionHelper.openAppDetailsSettings(context)
            },
            onRequestDeleteAll = {
                showSettingsDialog = false
                showDeleteAllDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // 個別削除の確認ダイアログ
    pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("記録を削除しますか？") },
            text = {
                val label = target.title.ifBlank { target.text.ifBlank { target.appName } }
                Text("「$label」の通知記録と保存済み画像を削除します。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        pendingDeleteItem = null
                        viewModel.deleteNotification(id) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("1 件の通知を削除しました")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 全件削除の確認ダイアログ
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("すべての記録を削除しますか？") },
            text = {
                Text("保存されている全 $totalCount 件の通知記録および内部ストレージの保存画像をすべて削除します。この操作は取り消せません。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllNotifications {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("すべての通知記録を削除しました")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("すべて削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 絞り込み中のエクスポート対象選択ダイアログ
    if (showExportScopeDialog) {
        AlertDialog(
            onDismissRequest = { showExportScopeDialog = false },
            title = { Text("JSON エクスポート") },
            text = {
                Text("現在フィルタが適用されています。すべての記録をエクスポートするか、表示中の絞り込み結果のみをエクスポートするか選択してください。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportScopeDialog = false
                        triggerExport(onlyFiltered = false)
                    }
                ) {
                    Text("全件 ($totalCount 件)")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExportScopeDialog = false }) {
                        Text("キャンセル")
                    }
                    TextButton(
                        onClick = {
                            showExportScopeDialog = false
                            triggerExport(onlyFiltered = true)
                        }
                    ) {
                        Text("絞り込み結果 (${notifications.size} 件)")
                    }
                }
            }
        )
    }

    // アプリ一覧ボトムシート
    if (showAppPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppPickerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "アプリで絞り込み",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ListItem(
                            headlineContent = { Text("すべてのアプリ") },
                            supportingContent = { Text("$totalCount 件") },
                            trailingContent = {
                                if (selectedPackageName == null) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.onSelectPackage(null)
                                showAppPickerSheet = false
                            }
                        )
                    }
                    items(recordedApps, key = { it.packageName }) { app ->
                        ListItem(
                            leadingContent = {
                                AppIconImage(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    size = 36.dp
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = app.appName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${app.packageName} • ${app.count} 件",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                if (selectedPackageName == app.packageName) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.onSelectPackage(app.packageName)
                                showAppPickerSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppFilterChipRow(
    recordedApps: List<AppSummary>,
    totalCount: Int,
    selectedPackageName: String?,
    onSelectPackage: (String?) -> Unit,
    onOpenAppSheet: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "__all__") {
            FilterChip(
                selected = selectedPackageName == null,
                onClick = { onSelectPackage(null) },
                label = { Text("すべて ($totalCount)") }
            )
        }

        if (recordedApps.size > 3) {
            item(key = "__picker__") {
                FilterChip(
                    selected = false,
                    onClick = onOpenAppSheet,
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("アプリ一覧") }
                )
            }
        }

        items(recordedApps, key = { it.packageName }) { app ->
            val selected = selectedPackageName == app.packageName
            FilterChip(
                selected = selected,
                onClick = {
                    onSelectPackage(if (selected) null else app.packageName)
                },
                leadingIcon = {
                    AppIconImage(
                        packageName = app.packageName,
                        appName = app.appName,
                        size = 18.dp
                    )
                },
                label = {
                    Text(
                        text = "${app.appName} (${app.count})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun NotificationListCard(
    item: NotificationWithImages,
    resolveImageFile: (String) -> File,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val n = item.notification
    val formattedTime = remember(n.postTime) {
        DateTimeFormatterUtil.formatListTimestamp(n.postTime)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppIconImage(
                packageName = n.packageName,
                appName = n.appName,
                size = 42.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = n.appName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (n.title.isNotBlank()) {
                    Text(
                        text = n.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "(タイトルなし)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (n.text.isNotBlank()) {
                    Text(
                        text = n.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.images.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "保存画像 ${item.images.size} 枚",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (item.images.isNotEmpty()) {
                val previewImage = item.images.first()
                Spacer(modifier = Modifier.width(8.dp))
                SavedNotificationThumbnail(
                    file = resolveImageFile(previewImage.fileName),
                    size = 44.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(
                onClick = onRequestDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "この記録を削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsPlaceholder(
    isFilterActive: Boolean,
    isAccessGranted: Boolean,
    onClearFilters: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MushroomBadgeIcon(size = 52.dp)
            if (isFilterActive) {
                Text(
                    text = "条件に一致する通知が見つかりません",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onClearFilters) {
                    Text("検索・絞り込みをクリア")
                }
            } else {
                Text(
                    text = "まだ記録された通知はありません",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isAccessGranted) {
                        "新しい通知が届くと自動的にここへ記録されます。"
                    } else {
                        "通知へのアクセスを許可すると、届く通知が自動的に記録されます。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isAccessGranted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onOpenSettings) {
                        Text("通知アクセス設定を開く")
                    }
                }
            }
        }
    }
}
