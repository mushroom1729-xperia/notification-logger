package com.mushroom.notificationlogger.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushroom.notificationlogger.ui.DetailUiState
import com.mushroom.notificationlogger.ui.DetailViewModel
import com.mushroom.notificationlogger.ui.components.AppIconImage
import com.mushroom.notificationlogger.ui.components.AppIcons
import com.mushroom.notificationlogger.ui.components.SavedNotificationImageCard
import com.mushroom.notificationlogger.ui.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val foundRecord = (uiState as? DetailUiState.Found)?.record

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("通知の詳細") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (foundRecord != null) {
                        IconButton(
                            onClick = {
                                val n = foundRecord.notification
                                val combined = buildString {
                                    if (n.title.isNotBlank()) appendLine(n.title)
                                    if (n.text.isNotBlank()) append(n.text)
                                }.trim()
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("Notification", combined)
                                )
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("テキストをクリップボードにコピーしました")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = AppIcons.ContentCopy,
                                contentDescription = "テキストをコピー"
                            )
                        }

                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = "この記録を削除"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            DetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "この通知記録は既に削除されています",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = onNavigateBack) {
                            Text("一覧へ戻る")
                        }
                    }
                }
            }

            is DetailUiState.Found -> {
                val n = state.record.notification
                val images = state.record.images

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // アプリ情報カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(
                                packageName = n.packageName,
                                appName = n.appName,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = n.appName,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = n.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 本文・タイトル全文カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "タイトル",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (n.title.isNotBlank()) {
                                    Text(
                                        text = n.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "(タイトルなし)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                Text(
                                    text = "本文",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (n.text.isNotBlank()) {
                                    Text(
                                        text = n.text,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    Text(
                                        text = "(本文なし)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 保存された画像一覧
                    if (images.isNotEmpty()) {
                        Text(
                            text = "保存された画像 (${images.size} 枚)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        images.forEach { imageEntity ->
                            SavedNotificationImageCard(
                                file = viewModel.resolveImageFile(imageEntity.fileName),
                                imageType = imageEntity.imageType,
                                width = imageEntity.width,
                                height = imageEntity.height
                            )
                        }
                    }

                    // メタデータカード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "記録メタ情報",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                MetadataRow(
                                    label = "投稿日時",
                                    value = DateTimeFormatterUtil.formatFullTimestamp(n.postTime)
                                )
                                MetadataRow(
                                    label = "記録・更新日時",
                                    value = DateTimeFormatterUtil.formatFullTimestamp(n.updatedAt)
                                )
                                MetadataRow(
                                    label = "カテゴリ",
                                    value = DateTimeFormatterUtil.formatCategoryLabel(n.category)
                                )
                                MetadataRow(
                                    label = "通知キー (sbn.key)",
                                    value = n.sbnKey
                                )
                                MetadataRow(
                                    label = "レコード ID",
                                    value = n.id.toString()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("この記録を削除しますか？") },
            text = {
                Text("表示中の通知記録と紐づく保存画像を削除します。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteRecord(onDeleted = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
