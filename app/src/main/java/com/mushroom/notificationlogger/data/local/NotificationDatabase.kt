package com.mushroom.notificationlogger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NotificationEntity::class,
        NotificationImageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NotificationDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val DB_NAME = "notification_logger.db"

        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getInstance(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
