package com.mushroom.notificationlogger.service

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import com.mushroom.notificationlogger.data.local.NotificationImageType
import com.mushroom.notificationlogger.data.storage.CapturedBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class ExtractedNotificationData(
    val sbnKey: String,
    val packageName: String,
    val appName: String,
    val postTime: Long,
    val title: String,
    val text: String,
    val category: String?,
    val images: List<CapturedBitmap>
)

object NotificationExtractor {

    private val appLabelCache = ConcurrentHashMap<String, String>()

    /**
     * グループまとめ通知や常駐フォアグラウンドサービス通知など、実通知でないものを除外判定する。
     */
    fun shouldSkipNotification(
        flags: Int,
        isOngoing: Boolean,
        category: String?,
        packageName: String,
        ownPackageName: String,
        excludeOngoing: Boolean
    ): Boolean {
        if (packageName == ownPackageName) return true

        // グループまとめ通知 (FLAG_GROUP_SUMMARY) は常に除外
        if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true

        if (excludeOngoing) {
            // バックグラウンド常駐サービスのステータス通知を除外
            if ((flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return true

            // 進行中バー・メディア再生などの常駐通知を除外（着信・アラームは残す）
            val ongoingFlag = isOngoing || (flags and Notification.FLAG_ONGOING_EVENT) != 0
            val isImportantAlert = category == Notification.CATEGORY_CALL ||
                category == Notification.CATEGORY_ALARM
            if (ongoingFlag && !isImportantAlert) return true
        }

        return false
    }

    fun extract(
        context: Context,
        sbn: StatusBarNotification,
        excludeOngoing: Boolean
    ): ExtractedNotificationData? {
        val notification = sbn.notification ?: return null
        val pkg = sbn.packageName ?: return null

        if (
            shouldSkipNotification(
                flags = notification.flags,
                isOngoing = sbn.isOngoing,
                category = notification.category,
                packageName = pkg,
                ownPackageName = context.packageName,
                excludeOngoing = excludeOngoing
            )
        ) {
            return null
        }

        val extras = notification.extras ?: Bundle.EMPTY
        val title = extractTitle(extras)
        val body = extractBody(notification, extras, title)
        val images = extractImages(context, notification, extras)

        // タイトル・本文・画像のすべてが空であるプレースホルダ通知は記録しない
        if (title.isBlank() && body.isBlank() && images.isEmpty()) {
            return null
        }

        val appName = resolveAppLabel(context, pkg)

        return ExtractedNotificationData(
            sbnKey = sbn.key,
            packageName = pkg,
            appName = appName,
            postTime = sbn.postTime,
            title = title,
            text = body,
            category = notification.category,
            images = images
        )
    }

    fun resolveAppLabel(context: Context, packageName: String): String {
        appLabelCache[packageName]?.let { return it }
        val resolved = runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().trim().takeIf { it.isNotEmpty() }
        }.getOrNull() ?: packageName
        appLabelCache[packageName] = resolved
        return resolved
    }

    private fun extractTitle(extras: Bundle): String {
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: ""

        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }

        return when {
            conversationTitle != null &&
                rawTitle.isNotEmpty() &&
                conversationTitle != rawTitle &&
                !rawTitle.startsWith(conversationTitle) -> "$conversationTitle ($rawTitle)"
            rawTitle.isNotEmpty() -> rawTitle
            conversationTitle != null -> conversationTitle
            else -> ""
        }
    }

    private fun extractBody(
        notification: Notification,
        extras: Bundle,
        resolvedTitle: String
    ): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (bigText != null) return bigText

        // MessagingStyle のメッセージ本文展開
        val messagingStyle = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        }.getOrNull()

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val lines = messagingStyle.messages.mapNotNull { msg ->
                val text = msg.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val sender = msg.person?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                if (sender != null && !resolvedTitle.contains(sender)) {
                    "$sender: $text"
                } else {
                    text
                }
            }
            if (lines.isNotEmpty()) {
                return lines.joinToString("\n")
            }
        }

        // InboxStyle の複数行展開
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
        if (textLines != null) return textLines

        val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (normalText != null) return normalText

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (subText != null) return subText

        return notification.tickerText?.toString()?.trim() ?: ""
    }

    private fun extractImages(
        context: Context,
        notification: Notification,
        extras: Bundle
    ): List<CapturedBitmap> {
        val result = mutableListOf<CapturedBitmap>()

        fun addDeduplicated(type: String, bitmap: Bitmap?) {
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return
            val duplicateIndex = result.indexOfFirst { existing ->
                existing.bitmap.width == bitmap.width &&
                    existing.bitmap.height == bitmap.height &&
                    runCatching { existing.bitmap.sameAs(bitmap) }.getOrDefault(false)
            }
            if (duplicateIndex >= 0) {
                // 同一画像が LARGE_ICON と BIG_PICTURE 両方に設定されている場合は BIG_PICTURE を優先
                if (
                    result[duplicateIndex].imageType == NotificationImageType.LARGE_ICON &&
                    type == NotificationImageType.BIG_PICTURE
                ) {
                    result[duplicateIndex] = CapturedBitmap(type, bitmap)
                }
                return
            }
            result.add(CapturedBitmap(type, bitmap))
        }

        // 1. 大きなアイコン (Large Icon)
        @Suppress("DEPRECATION")
        val largeIconBitmap = runCatching {
            notification.getLargeIcon()?.let { iconToBitmap(context, it) }
        }.getOrNull()
            ?: extractIconOrBitmapFromBundle(context, extras, Notification.EXTRA_LARGE_ICON_BIG)
            ?: extractIconOrBitmapFromBundle(context, extras, Notification.EXTRA_LARGE_ICON)
        addDeduplicated(NotificationImageType.LARGE_ICON, largeIconBitmap)

        // 2. BigPictureStyle の画像
        var bigPictureBitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bigPictureBitmap = runCatching {
                BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE_ICON, Icon::class.java)
                    ?.let { iconToBitmap(context, it) }
            }.getOrNull()
        }
        if (bigPictureBitmap == null) {
            bigPictureBitmap = extractIconOrBitmapFromBundle(context, extras, Notification.EXTRA_PICTURE)
        }
        addDeduplicated(NotificationImageType.BIG_PICTURE, bigPictureBitmap)

        // 3. MessagingStyle 内の添付画像 URI
        runCatching {
            val messagingStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null) {
                val allMessages = messagingStyle.historicMessages + messagingStyle.messages
                for (msg in allMessages) {
                    val mime = msg.dataMimeType ?: continue
                    val uri = msg.dataUri ?: continue
                    if (mime.startsWith("image/", ignoreCase = true)) {
                        val decoded = decodeSampledBitmapFromUri(context, uri, maxDimension = 2048)
                        addDeduplicated(NotificationImageType.MESSAGE_IMAGE, decoded)
                    }
                }
            }
        }

        return result
    }

    private fun extractIconOrBitmapFromBundle(
        context: Context,
        extras: Bundle,
        key: String
    ): Bitmap? {
        runCatching {
            BundleCompat.getParcelable(extras, key, Bitmap::class.java)?.let { return it }
        }
        runCatching {
            BundleCompat.getParcelable(extras, key, Icon::class.java)?.let {
                return iconToBitmap(context, it)
            }
        }
        return null
    }

    private fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
        return runCatching {
            val drawable = icon.loadDrawable(context) ?: return null
            drawableToBitmap(drawable)
        }.getOrNull()
    }

    fun drawableToBitmap(drawable: Drawable, fallbackSizePx: Int = 192): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: fallbackSizePx
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: fallbackSizePx
        return runCatching {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            while (max(rawW / sampleSize, rawH / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }.getOrNull()
    }
}
