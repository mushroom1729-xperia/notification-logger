package com.mushroom.notificationlogger.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object NotificationImageType {
    const val LARGE_ICON = "LARGE_ICON"
    const val BIG_PICTURE = "BIG_PICTURE"
    const val MESSAGE_IMAGE = "MESSAGE_IMAGE"
}

@Entity(
    tableName = "notification_images",
    foreignKeys = [
        ForeignKey(
            entity = NotificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["notificationId"])
    ]
)
data class NotificationImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val fileName: String,
    val imageType: String,
    val width: Int,
    val height: Int
)
