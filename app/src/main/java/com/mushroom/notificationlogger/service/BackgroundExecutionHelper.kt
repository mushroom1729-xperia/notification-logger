package com.mushroom.notificationlogger.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

enum class BatteryOptimizationState {
    /** バッテリー最適化の対象外（制限なし） */
    IGNORED,

    /** バッテリー最適化の対象（最適化が有効） */
    OPTIMIZED;

    val isIgnoringOptimizations: Boolean
        get() = this == IGNORED

    val shouldShowBanner: Boolean
        get() = this == OPTIMIZED

    val settingsStatusText: String
        get() = when (this) {
            IGNORED -> "対象外"
            OPTIMIZED -> "対象（最適化が有効）"
        }
}

enum class BackgroundRestrictionState {
    /** バックグラウンド制限なし */
    UNRESTRICTED,

    /** API 28+ でユーザーによりバックグラウンド動作が制限されている */
    RESTRICTED,

    /** API 28 未満のため対象外 */
    NOT_APPLICABLE;

    val isRestricted: Boolean
        get() = this == RESTRICTED

    val shouldWarn: Boolean
        get() = this == RESTRICTED

    val settingsStatusText: String
        get() = when (this) {
            UNRESTRICTED -> "制限なし"
            RESTRICTED -> "制限あり（バックグラウンド動作が制限されています）"
            NOT_APPLICABLE -> "対象外（Android 9 未満）"
        }
}

enum class ListenerConnectionUiState {
    /** 通知アクセス許可済み・サービス接続中（控えめに表示） */
    CONNECTED,

    /** 通知アクセス許可済みだがサービス切断中（警告バナー＋再バインド対象） */
    DISCONNECTED_NEEDS_REBIND,

    /** 通知アクセス未許可のため未接続（警告表示） */
    DISCONNECTED_NO_PERMISSION;

    val isConnected: Boolean
        get() = this == CONNECTED

    val isWarning: Boolean
        get() = this != CONNECTED
}

data class SystemHealthSnapshot(
    val isNotificationAccessGranted: Boolean,
    val isServiceConnected: Boolean,
    val batteryOptimizationState: BatteryOptimizationState,
    val backgroundRestrictionState: BackgroundRestrictionState
) {
    val listenerUiState: ListenerConnectionUiState =
        BackgroundExecutionHelper.resolveListenerConnectionState(
            isAccessGranted = isNotificationAccessGranted,
            isServiceConnected = isServiceConnected
        )

    val shouldAttemptRebind: Boolean =
        BackgroundExecutionHelper.shouldAttemptRebind(
            isAccessGranted = isNotificationAccessGranted,
            isServiceConnected = isServiceConnected
        )

    val hasPowerWarning: Boolean =
        batteryOptimizationState.shouldShowBanner || backgroundRestrictionState.shouldWarn
}

object BackgroundExecutionHelper {

    const val MIN_BACKGROUND_RESTRICTION_SDK: Int = Build.VERSION_CODES.P

    fun resolveBatteryOptimizationState(isIgnoringOptimizations: Boolean?): BatteryOptimizationState {
        return if (isIgnoringOptimizations == true) {
            BatteryOptimizationState.IGNORED
        } else {
            BatteryOptimizationState.OPTIMIZED
        }
    }

    fun resolveBackgroundRestrictionState(
        sdkInt: Int,
        isBackgroundRestricted: Boolean?
    ): BackgroundRestrictionState {
        if (sdkInt < MIN_BACKGROUND_RESTRICTION_SDK) {
            return BackgroundRestrictionState.NOT_APPLICABLE
        }
        return if (isBackgroundRestricted == true) {
            BackgroundRestrictionState.RESTRICTED
        } else {
            BackgroundRestrictionState.UNRESTRICTED
        }
    }

    fun resolveListenerConnectionState(
        isAccessGranted: Boolean,
        isServiceConnected: Boolean
    ): ListenerConnectionUiState {
        return when {
            !isAccessGranted -> ListenerConnectionUiState.DISCONNECTED_NO_PERMISSION
            isServiceConnected -> ListenerConnectionUiState.CONNECTED
            else -> ListenerConnectionUiState.DISCONNECTED_NEEDS_REBIND
        }
    }

    fun shouldAttemptRebind(
        isAccessGranted: Boolean,
        isServiceConnected: Boolean
    ): Boolean {
        return isAccessGranted && !isServiceConnected
    }

    fun evaluateSystemHealth(
        sdkInt: Int,
        isNotificationAccessGranted: Boolean,
        isServiceConnected: Boolean,
        isIgnoringBatteryOptimizations: Boolean?,
        isBackgroundRestricted: Boolean?
    ): SystemHealthSnapshot {
        return SystemHealthSnapshot(
            isNotificationAccessGranted = isNotificationAccessGranted,
            isServiceConnected = isServiceConnected,
            batteryOptimizationState = resolveBatteryOptimizationState(isIgnoringBatteryOptimizations),
            backgroundRestrictionState = resolveBackgroundRestrictionState(
                sdkInt = sdkInt,
                isBackgroundRestricted = isBackgroundRestricted
            )
        )
    }

    fun executeSettingsNavigationWithFallback(
        primaryAction: String,
        primaryPackageName: String,
        fallbackAction: String,
        starter: (action: String, packageName: String?) -> Boolean
    ): Boolean {
        if (starter(primaryAction, primaryPackageName)) {
            return true
        }
        return starter(fallbackAction, null)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return runCatching {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        }.getOrDefault(false)
    }

    fun getBatteryOptimizationState(context: Context): BatteryOptimizationState {
        val raw = runCatching {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrNull()
        return resolveBatteryOptimizationState(raw)
    }

    fun isBackgroundRestricted(context: Context): Boolean {
        return getBackgroundRestrictionState(context).isRestricted
    }

    fun getBackgroundRestrictionState(context: Context): BackgroundRestrictionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return BackgroundRestrictionState.NOT_APPLICABLE
        }
        val restricted = runCatching {
            context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted
        }.getOrNull()
        return resolveBackgroundRestrictionState(Build.VERSION.SDK_INT, restricted)
    }

    fun readSystemHealthSnapshot(
        context: Context,
        isServiceConnected: Boolean = NotificationCaptureService.isServiceConnected.value
    ): SystemHealthSnapshot {
        val accessGranted = NotificationAccessHelper.isNotificationAccessGranted(context)
        val ignoringBatteryOpt = runCatching {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrNull()
        val backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted
            }.getOrNull()
        } else {
            null
        }
        return evaluateSystemHealth(
            sdkInt = Build.VERSION.SDK_INT,
            isNotificationAccessGranted = accessGranted,
            isServiceConnected = isServiceConnected,
            isIgnoringBatteryOptimizations = ignoringBatteryOpt,
            isBackgroundRestricted = backgroundRestricted
        )
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        return executeSettingsNavigationWithFallback(
            primaryAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            primaryPackageName = context.packageName,
            fallbackAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            starter = { action, pkg -> startSettingsIntent(context, action, pkg) }
        )
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return startSettingsIntent(
            context = context,
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            packageName = null
        )
    }

    fun openAppDetailsSettings(context: Context): Boolean {
        return executeSettingsNavigationWithFallback(
            primaryAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            primaryPackageName = context.packageName,
            fallbackAction = Settings.ACTION_APPLICATION_SETTINGS,
            starter = { action, pkg -> startSettingsIntent(context, action, pkg) }
        )
    }

    private fun startSettingsIntent(
        context: Context,
        action: String,
        packageName: String?
    ): Boolean {
        val intent = if (packageName != null) {
            Intent(action, Uri.fromParts("package", packageName, null))
        } else {
            Intent(action)
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
        }.isSuccess
    }
}
