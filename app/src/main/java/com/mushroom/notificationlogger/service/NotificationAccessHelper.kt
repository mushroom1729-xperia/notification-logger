package com.mushroom.notificationlogger.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

object NotificationAccessHelper {

    fun isNotificationAccessGranted(context: Context): Boolean {
        return runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }.getOrDefault(false)
    }

    fun openNotificationAccessSettings(context: Context) {
        val componentName = ComponentName(context, NotificationCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    componentName.flattenToString()
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(detailIntent) }.isSuccess) {
                return
            }
        }

        val fallbackIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(fallbackIntent)
        }
    }

    fun ensureListenerBound(context: Context): Boolean {
        if (!isNotificationAccessGranted(context)) return false
        return runCatching {
            val componentName = ComponentName(
                context.applicationContext,
                NotificationCaptureService::class.java
            )
            NotificationListenerService.requestRebind(componentName)
            true
        }.getOrDefault(false)
    }
}
