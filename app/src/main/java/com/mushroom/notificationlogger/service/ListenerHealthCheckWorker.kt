package com.mushroom.notificationlogger.service

import android.content.Context
import android.os.UserManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mushroom.notificationlogger.NotificationLoggerApp
import java.util.concurrent.TimeUnit

class ListenerHealthCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val isConnected = NotificationCaptureService.isServiceConnected.value
            val snapshot = BackgroundExecutionHelper.readSystemHealthSnapshot(
                context = applicationContext,
                isServiceConnected = isConnected
            )

            var rebindRequested = false
            if (snapshot.shouldAttemptRebind) {
                rebindRequested = NotificationAccessHelper.ensureListenerBound(applicationContext)
                Log.w(
                    TAG,
                    "Listener was disconnected during periodic check; requested rebind=$rebindRequested"
                )
            } else {
                Log.i(
                    TAG,
                    "Periodic check completed: connected=${snapshot.isServiceConnected}, " +
                        "batteryOpt=${snapshot.batteryOptimizationState}, " +
                        "bgRestriction=${snapshot.backgroundRestrictionState}"
                )
            }

            val app = applicationContext as? NotificationLoggerApp
            app?.preferencesRepository?.recordHealthCheckResult(
                checkedAtMillis = System.currentTimeMillis(),
                wasConnected = snapshot.isServiceConnected,
                isIgnoringBatteryOptimizations =
                    snapshot.batteryOptimizationState.isIgnoringOptimizations,
                isBackgroundRestricted = snapshot.backgroundRestrictionState.isRestricted,
                rebindRequested = rebindRequested
            )

            Result.success()
        }.getOrElse { err ->
            Log.w(TAG, "Failed to run periodic listener health check", err)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ListenerHealthWorker"
        const val UNIQUE_WORK_NAME = "notification_listener_health_check"
        const val CHECK_INTERVAL_HOURS = 6L

        fun schedulePeriodicCheck(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                val userManager = appContext.getSystemService(UserManager::class.java)
                if (userManager != null && !userManager.isUserUnlocked) {
                    return
                }

                val request = PeriodicWorkRequestBuilder<ListenerHealthCheckWorker>(
                    CHECK_INTERVAL_HOURS,
                    TimeUnit.HOURS
                ).build()

                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }.onFailure { err ->
                Log.w(TAG, "Failed to schedule periodic health check", err)
            }
        }
    }
}
