package com.mushroom.notificationlogger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceRecoveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            NotificationAccessHelper.ensureListenerBound(context)
            ListenerHealthCheckWorker.schedulePeriodicCheck(context)
        }
    }
}
