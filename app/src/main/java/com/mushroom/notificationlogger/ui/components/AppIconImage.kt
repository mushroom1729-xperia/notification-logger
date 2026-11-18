package com.mushroom.notificationlogger.ui.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushroom.notificationlogger.service.NotificationExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppIconMemoryCache {
    private val cache = LruCache<String, ImageBitmap>(128)

    fun get(packageName: String): ImageBitmap? = synchronized(cache) {
        cache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) = synchronized(cache) {
        cache.put(packageName, bitmap)
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val cached = AppIconMemoryCache.get(packageName)

    val iconBitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = packageName) {
        if (value != null) return@produceState
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                NotificationExtractor.drawableToBitmap(drawable, fallbackSizePx = 128)
                    ?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) {
            AppIconMemoryCache.put(packageName, loaded)
            value = loaded
        }
    }

    val shape = RoundedCornerShape((size.value * 0.24f).dp)
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = appName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            val initial = appName.trim().firstOrNull()?.uppercase() ?: "?"
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
