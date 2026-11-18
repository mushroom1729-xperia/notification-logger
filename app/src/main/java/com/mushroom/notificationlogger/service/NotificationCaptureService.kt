package com.mushroom.notificationlogger.service

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mushroom.notificationlogger.NotificationLoggerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true

        // サービス再起動・再バインド時にステータスバー上の既存アクティブ通知を取りこぼさず同期する
        serviceScope.launch {
            runCatching {
                val currentActive = activeNotifications ?: return@runCatching
                val app = applicationContext as? NotificationLoggerApp ?: return@runCatching
                val excludeOngoing = app.preferencesRepository.excludeOngoingFlow.first()
                for (sbn in currentActive) {
                    processStatusBarNotification(app, sbn, excludeOngoing)
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to sync active notifications on connect", err)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val target = sbn ?: return
        serviceScope.launch {
            runCatching {
                val app = applicationContext as? NotificationLoggerApp ?: return@launch
                val excludeOngoing = app.preferencesRepository.excludeOngoingFlow.first()
                processStatusBarNotification(app, target, excludeOngoing)
            }.onFailure { err ->
                Log.w(TAG, "Failed to record posted notification", err)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        runCatching {
            requestRebind(ComponentName(applicationContext, NotificationCaptureService::class.java))
        }.onFailure { err ->
            Log.w(TAG, "Failed to request listener rebind", err)
        }
    }

    override fun onDestroy() {
        _isServiceConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun processStatusBarNotification(
        app: NotificationLoggerApp,
        sbn: StatusBarNotification,
        excludeOngoing: Boolean
    ) {
        val extracted = NotificationExtractor.extract(
            context = this,
            sbn = sbn,
            excludeOngoing = excludeOngoing
        ) ?: return

        app.notificationRepository.recordOrUpdateNotification(
            sbnKey = extracted.sbnKey,
            packageName = extracted.packageName,
            appName = extracted.appName,
            postTime = extracted.postTime,
            title = extracted.title,
            text = extracted.text,
            category = extracted.category,
            capturedImages = extracted.images
        )
    }

    companion object {
        private const val TAG = "NotifCaptureService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }
}
