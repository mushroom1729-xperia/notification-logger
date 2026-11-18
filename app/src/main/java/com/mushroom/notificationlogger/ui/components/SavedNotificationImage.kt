package com.mushroom.notificationlogger.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mushroom.notificationlogger.data.local.NotificationImageType
import com.mushroom.notificationlogger.ui.util.DateTimeFormatterUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SavedNotificationThumbnail(
    file: File,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching {
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val loaded = bitmap ?: return
    Image(
        bitmap = loaded,
        contentDescription = file.name,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun SavedNotificationImageCard(
    file: File,
    imageType: String,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier
) {
    var showPreviewDialog by remember { mutableStateOf(false) }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeFormatterUtil.formatImageTypeLabel(imageType),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (width > 0 && height > 0) {
                    Text(
                        text = "${width}×${height}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (imageBitmap != null) {
                val isIcon = imageType == NotificationImageType.LARGE_ICON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                        .clickable { showPreviewDialog = true }
                        .padding(if (isIcon) 12.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = file.name,
                        contentScale = if (isIcon) ContentScale.Fit else ContentScale.FillWidth,
                        modifier = if (isIcon) {
                            Modifier.size(96.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                        }
                    )
                }
            } else {
                Text(
                    text = "画像ファイルを読み込めませんでした (${file.name})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "ファイル名: ${file.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showPreviewDialog && imageBitmap != null) {
        Dialog(
            onDismissRequest = { showPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPreviewDialog = false }) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }
    }
}
