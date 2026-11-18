package com.mushroom.notificationlogger.ui.util

import android.app.Notification
import com.mushroom.notificationlogger.data.local.NotificationImageType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeFormatterUtil {

    private val timeOnlyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthDayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    private val yearMonthDayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    private val fullDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun formatListTimestamp(
        epochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val target = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        return when {
            target.toLocalDate() == now.toLocalDate() -> target.format(timeOnlyFormatter)
            target.year == now.year -> target.format(monthDayTimeFormatter)
            else -> target.format(yearMonthDayTimeFormatter)
        }
    }

    fun formatFullTimestamp(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(fullDateTimeFormatter)
    }

    fun formatCategoryLabel(category: String?): String {
        if (category.isNullOrBlank()) return "なし"
        val jp = when (category) {
            Notification.CATEGORY_MESSAGE -> "メッセージ"
            Notification.CATEGORY_EMAIL -> "メール"
            Notification.CATEGORY_SOCIAL -> "SNS"
            Notification.CATEGORY_CALL -> "着信・通話"
            Notification.CATEGORY_ALARM -> "アラーム"
            Notification.CATEGORY_REMINDER -> "リマインダー"
            Notification.CATEGORY_EVENT -> "予定・イベント"
            Notification.CATEGORY_PROMO -> "プロモーション"
            Notification.CATEGORY_PROGRESS -> "進行状況"
            Notification.CATEGORY_STATUS -> "ステータス"
            Notification.CATEGORY_SYSTEM -> "システム"
            Notification.CATEGORY_SERVICE -> "サービス"
            Notification.CATEGORY_TRANSPORT -> "メディア再生"
            Notification.CATEGORY_ERROR -> "エラー"
            Notification.CATEGORY_RECOMMENDATION -> "おすすめ"
            Notification.CATEGORY_WORKOUT -> "ワークアウト"
            Notification.CATEGORY_LOCATION_SHARING -> "位置情報共有"
            Notification.CATEGORY_STOPWATCH -> "ストップウォッチ"
            Notification.CATEGORY_MISSED_CALL -> "不在着信"
            else -> null
        }
        return if (jp != null) "$jp ($category)" else category
    }

    fun formatImageTypeLabel(imageType: String): String {
        return when (imageType) {
            NotificationImageType.LARGE_ICON -> "大きなアイコン (Large Icon)"
            NotificationImageType.BIG_PICTURE -> "展開画像 (BigPicture)"
            NotificationImageType.MESSAGE_IMAGE -> "添付画像 (Message)"
            else -> imageType
        }
    }
}
