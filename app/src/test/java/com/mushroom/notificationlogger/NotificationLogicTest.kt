package com.mushroom.notificationlogger

import android.app.Notification
import com.mushroom.notificationlogger.data.local.NotificationEntity
import com.mushroom.notificationlogger.data.local.NotificationImageEntity
import com.mushroom.notificationlogger.data.local.NotificationImageType
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.data.repository.NotificationRepository
import com.mushroom.notificationlogger.export.ExportEnvelope
import com.mushroom.notificationlogger.export.NotificationExporter
import com.mushroom.notificationlogger.service.NotificationExtractor
import com.mushroom.notificationlogger.ui.util.DateTimeFormatterUtil
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class NotificationLogicTest {

    @Test
    fun `group summary notifications are always excluded`() {
        val excluded = NotificationExtractor.shouldSkipNotification(
            flags = Notification.FLAG_GROUP_SUMMARY,
            isOngoing = false,
            category = Notification.CATEGORY_MESSAGE,
            packageName = "com.example.chat",
            ownPackageName = "com.mushroom.notificationlogger",
            excludeOngoing = false
        )
        assertTrue(excluded)
    }

    @Test
    fun `own package notifications are excluded`() {
        val excluded = NotificationExtractor.shouldSkipNotification(
            flags = 0,
            isOngoing = false,
            category = null,
            packageName = "com.mushroom.notificationlogger",
            ownPackageName = "com.mushroom.notificationlogger",
            excludeOngoing = true
        )
        assertTrue(excluded)
    }

    @Test
    fun `ongoing notifications are excluded except incoming calls and alarms`() {
        val mediaOngoing = NotificationExtractor.shouldSkipNotification(
            flags = Notification.FLAG_ONGOING_EVENT,
            isOngoing = true,
            category = Notification.CATEGORY_TRANSPORT,
            packageName = "com.example.music",
            ownPackageName = "com.mushroom.notificationlogger",
            excludeOngoing = true
        )
        assertTrue(mediaOngoing)

        val incomingCall = NotificationExtractor.shouldSkipNotification(
            flags = Notification.FLAG_ONGOING_EVENT,
            isOngoing = true,
            category = Notification.CATEGORY_CALL,
            packageName = "com.example.dialer",
            ownPackageName = "com.mushroom.notificationlogger",
            excludeOngoing = true
        )
        assertFalse(incomingCall)

        val regularMessage = NotificationExtractor.shouldSkipNotification(
            flags = Notification.FLAG_AUTO_CANCEL,
            isOngoing = false,
            category = Notification.CATEGORY_MESSAGE,
            packageName = "com.example.chat",
            ownPackageName = "com.mushroom.notificationlogger",
            excludeOngoing = true
        )
        assertFalse(regularMessage)
    }

    @Test
    fun `escapeLikeQuery escapes wildcards and backslashes`() {
        assertEquals(
            """100\% \_test\\path""",
            NotificationRepository.escapeLikeQuery("""100% _test\path""")
        )
    }

    @Test
    fun `export json references images strictly by file name`() {
        val sample = NotificationWithImages(
            notification = NotificationEntity(
                id = 42L,
                sbnKey = "0|com.example.chat|1001|null|10234",
                packageName = "com.example.chat",
                appName = "Example Chat",
                postTime = 1795000000000L,
                updatedAt = 1795000005000L,
                title = "テスト送信者",
                text = "画像のテストメッセージです",
                category = "msg"
            ),
            images = listOf(
                NotificationImageEntity(
                    id = 7L,
                    notificationId = 42L,
                    fileName = "/data/user/0/com.mushroom.notificationlogger/files/notification_images/notif_42_big_picture_0_abcd.png",
                    imageType = NotificationImageType.BIG_PICTURE,
                    width = 1080,
                    height = 720
                )
            )
        )

        val jsonText = NotificationExporter.serializeNotificationsToJson(
            records = listOf(sample),
            exportedAtMillis = 1795000100000L
        )
        val decoded = Json.decodeFromString<ExportEnvelope>(jsonText)

        assertEquals(1, decoded.count)
        assertEquals(1, decoded.notifications.size)
        val exportedImg = decoded.notifications.first().images.first()
        assertEquals("notif_42_big_picture_0_abcd.png", exportedImg.fileName)
        assertFalse(exportedImg.fileName.contains("/"))
        assertFalse(exportedImg.fileName.contains("\\"))
    }

    @Test
    fun `theme mode storage key round-trip`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("SYSTEM"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey(null))
    }

    @Test
    fun `timestamp formatting distinguishes same day and full format`() {
        val zone = ZoneId.of("Asia/Tokyo")
        // 2026-11-18 11:05:30 JST
        val target = 1794967530000L
        val sameDayLater = target + 3_600_000L
        val listText = DateTimeFormatterUtil.formatListTimestamp(target, sameDayLater, zone)
        assertTrue(listText.matches(Regex("""\d{2}:\d{2}""")))

        val fullText = DateTimeFormatterUtil.formatFullTimestamp(target, zone)
        assertTrue(fullText.matches(Regex("""\d{4}/\d{2}/\d{2} \d{2}:\d{2}:\d{2}""")))
    }
}
