package com.mushroom.notificationlogger.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class NotificationWithImages(
    @Embedded
    val notification: NotificationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "notificationId"
    )
    val images: List<NotificationImageEntity>
)

data class AppSummary(
    val packageName: String,
    val appName: String,
    val count: Int
)
