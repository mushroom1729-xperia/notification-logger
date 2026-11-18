package com.mushroom.notificationlogger.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["sbnKey", "postTime"], unique = true),
        Index(value = ["postTime"]),
        Index(value = ["packageName"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sbnKey: String,
    val packageName: String,
    val appName: String,
    val postTime: Long,
    val updatedAt: Long,
    val title: String,
    val text: String,
    val category: String?
)
