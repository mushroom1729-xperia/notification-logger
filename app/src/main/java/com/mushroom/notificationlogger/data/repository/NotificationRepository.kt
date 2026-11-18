package com.mushroom.notificationlogger.data.repository

import androidx.room.withTransaction
import com.mushroom.notificationlogger.data.local.AppSummary
import com.mushroom.notificationlogger.data.local.NotificationDatabase
import com.mushroom.notificationlogger.data.local.NotificationEntity
import com.mushroom.notificationlogger.data.local.NotificationImageEntity
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import com.mushroom.notificationlogger.data.storage.CapturedBitmap
import com.mushroom.notificationlogger.data.storage.ImageStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class NotificationRepository(
    private val database: NotificationDatabase,
    private val imageStorageManager: ImageStorageManager
) {
    private val dao = database.notificationDao()
    private val upsertMutex = Mutex()

    fun observeNotifications(
        packageName: String?,
        rawQuery: String
    ): Flow<List<NotificationWithImages>> {
        val escaped = escapeLikeQuery(rawQuery.trim())
        return dao.observeNotifications(packageName = packageName, escapedQuery = escaped)
    }

    fun observeNotificationById(id: Long): Flow<NotificationWithImages?> {
        return dao.observeNotificationById(id)
    }

    fun observeRecordedApps(): Flow<List<AppSummary>> {
        return dao.observeRecordedApps()
    }

    fun observeTotalCount(): Flow<Int> {
        return dao.observeTotalCount()
    }

    suspend fun getAllNotificationsForExport(): List<NotificationWithImages> {
        return dao.getAllNotificationsWithImages()
    }

    fun resolveImageFile(fileName: String): File {
        return imageStorageManager.resolveImageFile(fileName)
    }

    /**
     * sbn.key と postTime の組で既存レコードを検索し、存在する場合は同一 ID のまま上書き更新する。
     */
    suspend fun recordOrUpdateNotification(
        sbnKey: String,
        packageName: String,
        appName: String,
        postTime: Long,
        title: String,
        text: String,
        category: String?,
        capturedImages: List<CapturedBitmap>,
        nowMillis: Long = System.currentTimeMillis()
    ): Long = upsertMutex.withLock {
        var obsoleteFilesToDelete: List<String> = emptyList()

        val targetId = database.withTransaction {
            val existing = dao.findByKeyAndPostTime(sbnKey = sbnKey, postTime = postTime)
            val notificationId = if (existing != null) {
                val updated = existing.copy(
                    packageName = packageName,
                    appName = appName,
                    updatedAt = nowMillis,
                    title = title,
                    text = text,
                    category = category
                )
                dao.updateNotification(updated)
                existing.id
            } else {
                dao.insertNotification(
                    NotificationEntity(
                        sbnKey = sbnKey,
                        packageName = packageName,
                        appName = appName,
                        postTime = postTime,
                        updatedAt = nowMillis,
                        title = title,
                        text = text,
                        category = category
                    )
                )
            }

            if (capturedImages.isNotEmpty()) {
                val oldImages = dao.getImagesForNotification(notificationId)
                if (oldImages.isNotEmpty()) {
                    obsoleteFilesToDelete = oldImages.map { it.fileName }
                    dao.deleteImagesForNotification(notificationId)
                }

                val savedEntities = capturedImages.mapIndexedNotNull { index, item ->
                    val meta = imageStorageManager.saveBitmap(
                        notificationId = notificationId,
                        imageType = item.imageType,
                        index = index,
                        sourceBitmap = item.bitmap
                    )
                    meta?.let {
                        NotificationImageEntity(
                            notificationId = notificationId,
                            fileName = it.fileName,
                            imageType = it.imageType,
                            width = it.width,
                            height = it.height
                        )
                    }
                }
                if (savedEntities.isNotEmpty()) {
                    dao.insertImages(savedEntities)
                }
            }

            notificationId
        }

        if (obsoleteFilesToDelete.isNotEmpty()) {
            imageStorageManager.deleteFiles(obsoleteFilesToDelete)
        }

        targetId
    }

    suspend fun deleteNotification(id: Long) = upsertMutex.withLock {
        val images = dao.getImagesForNotification(id)
        dao.deleteNotificationById(id)
        if (images.isNotEmpty()) {
            imageStorageManager.deleteFiles(images.map { it.fileName })
        }
    }

    suspend fun deleteAllNotifications() = upsertMutex.withLock {
        dao.deleteAllNotifications()
        imageStorageManager.deleteAllFiles()
    }

    companion object {
        fun escapeLikeQuery(input: String): String {
            if (input.isEmpty()) return ""
            return buildString(input.length) {
                for (ch in input) {
                    when (ch) {
                        '\\', '%', '_' -> {
                            append('\\')
                            append(ch)
                        }
                        else -> append(ch)
                    }
                }
            }
        }
    }
}
