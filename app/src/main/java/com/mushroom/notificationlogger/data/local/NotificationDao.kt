package com.mushroom.notificationlogger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Transaction
    @Query(
        """
        SELECT * FROM notifications
        WHERE (:packageName IS NULL OR packageName = :packageName)
          AND (
            :escapedQuery = ''
            OR title LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR text LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR appName LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR packageName LIKE '%' || :escapedQuery || '%' ESCAPE '\'
          )
        ORDER BY postTime DESC, id DESC
        """
    )
    fun observeNotifications(
        packageName: String?,
        escapedQuery: String
    ): Flow<List<NotificationWithImages>>

    @Transaction
    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    fun observeNotificationById(id: Long): Flow<NotificationWithImages?>

    @Transaction
    @Query("SELECT * FROM notifications ORDER BY postTime DESC, id DESC")
    suspend fun getAllNotificationsWithImages(): List<NotificationWithImages>

    @Query(
        """
        SELECT packageName, MAX(appName) AS appName, COUNT(*) AS count
        FROM notifications
        GROUP BY packageName
        ORDER BY MAX(postTime) DESC
        """
    )
    fun observeRecordedApps(): Flow<List<AppSummary>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE sbnKey = :sbnKey AND postTime = :postTime LIMIT 1")
    suspend fun findByKeyAndPostTime(sbnKey: String, postTime: Long): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotification(entity: NotificationEntity): Long

    @Update
    suspend fun updateNotification(entity: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<NotificationImageEntity>)

    @Query("SELECT * FROM notification_images WHERE notificationId = :notificationId")
    suspend fun getImagesForNotification(notificationId: Long): List<NotificationImageEntity>

    @Query("DELETE FROM notification_images WHERE notificationId = :notificationId")
    suspend fun deleteImagesForNotification(notificationId: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}
