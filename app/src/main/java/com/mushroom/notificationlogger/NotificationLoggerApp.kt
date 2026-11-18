package com.mushroom.notificationlogger

import android.app.Application
import com.mushroom.notificationlogger.data.local.NotificationDatabase
import com.mushroom.notificationlogger.data.preferences.UserPreferencesRepository
import com.mushroom.notificationlogger.data.repository.NotificationRepository
import com.mushroom.notificationlogger.data.storage.ImageStorageManager
import com.mushroom.notificationlogger.service.ListenerHealthCheckWorker
import com.mushroom.notificationlogger.service.NotificationAccessHelper

class NotificationLoggerApp : Application() {

    val database: NotificationDatabase by lazy {
        NotificationDatabase.getInstance(this)
    }

    val imageStorageManager: ImageStorageManager by lazy {
        ImageStorageManager(this)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(
            database = database,
            imageStorageManager = imageStorageManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            NotificationAccessHelper.ensureListenerBound(this)
        }
        ListenerHealthCheckWorker.schedulePeriodicCheck(this)
    }
}
