package com.mushroom.notificationlogger

import android.os.Build
import android.provider.Settings
import com.mushroom.notificationlogger.service.BackgroundExecutionHelper
import com.mushroom.notificationlogger.service.BackgroundRestrictionState
import com.mushroom.notificationlogger.service.BatteryOptimizationState
import com.mushroom.notificationlogger.service.ListenerConnectionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackgroundExecutionLogicTest {

    @Test
    fun `battery optimization state distinguishes ignored and optimized`() {
        val ignored = BackgroundExecutionHelper.resolveBatteryOptimizationState(true)
        assertEquals(BatteryOptimizationState.IGNORED, ignored)
        assertTrue(ignored.isIgnoringOptimizations)
        assertFalse(ignored.shouldShowBanner)
        assertEquals("対象外", ignored.settingsStatusText)

        val optimized = BackgroundExecutionHelper.resolveBatteryOptimizationState(false)
        assertEquals(BatteryOptimizationState.OPTIMIZED, optimized)
        assertFalse(optimized.isIgnoringOptimizations)
        assertTrue(optimized.shouldShowBanner)
        assertEquals("対象（最適化が有効）", optimized.settingsStatusText)

        val unknown = BackgroundExecutionHelper.resolveBatteryOptimizationState(null)
        assertEquals(BatteryOptimizationState.OPTIMIZED, unknown)
        assertTrue(unknown.shouldShowBanner)
    }

    @Test
    fun `background restriction state respects API 28 threshold`() {
        val api26 = BackgroundExecutionHelper.resolveBackgroundRestrictionState(
            sdkInt = Build.VERSION_CODES.O,
            isBackgroundRestricted = true
        )
        assertEquals(BackgroundRestrictionState.NOT_APPLICABLE, api26)
        assertFalse(api26.isRestricted)
        assertFalse(api26.shouldWarn)

        val api27 = BackgroundExecutionHelper.resolveBackgroundRestrictionState(
            sdkInt = Build.VERSION_CODES.O_MR1,
            isBackgroundRestricted = null
        )
        assertEquals(BackgroundRestrictionState.NOT_APPLICABLE, api27)
        assertFalse(api27.shouldWarn)

        val api28Unrestricted = BackgroundExecutionHelper.resolveBackgroundRestrictionState(
            sdkInt = Build.VERSION_CODES.P,
            isBackgroundRestricted = false
        )
        assertEquals(BackgroundRestrictionState.UNRESTRICTED, api28Unrestricted)
        assertFalse(api28Unrestricted.isRestricted)
        assertFalse(api28Unrestricted.shouldWarn)

        val api28Restricted = BackgroundExecutionHelper.resolveBackgroundRestrictionState(
            sdkInt = Build.VERSION_CODES.P,
            isBackgroundRestricted = true
        )
        assertEquals(BackgroundRestrictionState.RESTRICTED, api28Restricted)
        assertTrue(api28Restricted.isRestricted)
        assertTrue(api28Restricted.shouldWarn)

        val api35Null = BackgroundExecutionHelper.resolveBackgroundRestrictionState(
            sdkInt = 35,
            isBackgroundRestricted = null
        )
        assertEquals(BackgroundRestrictionState.UNRESTRICTED, api35Null)
    }

    @Test
    fun `listener connection ui state reflects permission and connection status`() {
        val connected = BackgroundExecutionHelper.resolveListenerConnectionState(
            isAccessGranted = true,
            isServiceConnected = true
        )
        assertEquals(ListenerConnectionUiState.CONNECTED, connected)
        assertTrue(connected.isConnected)
        assertFalse(connected.isWarning)

        val disconnectedWithPermission = BackgroundExecutionHelper.resolveListenerConnectionState(
            isAccessGranted = true,
            isServiceConnected = false
        )
        assertEquals(
            ListenerConnectionUiState.DISCONNECTED_NEEDS_REBIND,
            disconnectedWithPermission
        )
        assertFalse(disconnectedWithPermission.isConnected)
        assertTrue(disconnectedWithPermission.isWarning)

        val noPermission = BackgroundExecutionHelper.resolveListenerConnectionState(
            isAccessGranted = false,
            isServiceConnected = false
        )
        assertEquals(ListenerConnectionUiState.DISCONNECTED_NO_PERMISSION, noPermission)
        assertFalse(noPermission.isConnected)
        assertTrue(noPermission.isWarning)
    }

    @Test
    fun `rebind is requested only when permission is granted and listener is disconnected`() {
        assertTrue(
            BackgroundExecutionHelper.shouldAttemptRebind(
                isAccessGranted = true,
                isServiceConnected = false
            )
        )
        assertFalse(
            BackgroundExecutionHelper.shouldAttemptRebind(
                isAccessGranted = true,
                isServiceConnected = true
            )
        )
        assertFalse(
            BackgroundExecutionHelper.shouldAttemptRebind(
                isAccessGranted = false,
                isServiceConnected = false
            )
        )
    }

    @Test
    fun `evaluateSystemHealth aggregates rebind and power warning flags`() {
        val healthy = BackgroundExecutionHelper.evaluateSystemHealth(
            sdkInt = 35,
            isNotificationAccessGranted = true,
            isServiceConnected = true,
            isIgnoringBatteryOptimizations = true,
            isBackgroundRestricted = false
        )
        assertEquals(ListenerConnectionUiState.CONNECTED, healthy.listenerUiState)
        assertFalse(healthy.shouldAttemptRebind)
        assertFalse(healthy.hasPowerWarning)

        val needsRebindAndBatteryOpt = BackgroundExecutionHelper.evaluateSystemHealth(
            sdkInt = 35,
            isNotificationAccessGranted = true,
            isServiceConnected = false,
            isIgnoringBatteryOptimizations = false,
            isBackgroundRestricted = false
        )
        assertEquals(
            ListenerConnectionUiState.DISCONNECTED_NEEDS_REBIND,
            needsRebindAndBatteryOpt.listenerUiState
        )
        assertTrue(needsRebindAndBatteryOpt.shouldAttemptRebind)
        assertTrue(needsRebindAndBatteryOpt.hasPowerWarning)

        val backgroundRestrictedOnly = BackgroundExecutionHelper.evaluateSystemHealth(
            sdkInt = Build.VERSION_CODES.P,
            isNotificationAccessGranted = true,
            isServiceConnected = true,
            isIgnoringBatteryOptimizations = true,
            isBackgroundRestricted = true
        )
        assertFalse(backgroundRestrictedOnly.shouldAttemptRebind)
        assertTrue(backgroundRestrictedOnly.hasPowerWarning)
        assertEquals(
            BackgroundRestrictionState.RESTRICTED,
            backgroundRestrictedOnly.backgroundRestrictionState
        )
    }

    @Test
    fun `settings navigation executes primary package action and falls back on failure`() {
        val primaryCalls = mutableListOf<Pair<String, String?>>()
        val primaryOk = BackgroundExecutionHelper.executeSettingsNavigationWithFallback(
            primaryAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            primaryPackageName = "com.mushroom.notificationlogger",
            fallbackAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        ) { action, pkg ->
            primaryCalls.add(action to pkg)
            true
        }
        assertTrue(primaryOk)
        assertEquals(
            listOf(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS to
                    "com.mushroom.notificationlogger"
            ),
            primaryCalls
        )

        val fallbackCalls = mutableListOf<Pair<String, String?>>()
        val fallbackOk = BackgroundExecutionHelper.executeSettingsNavigationWithFallback(
            primaryAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            primaryPackageName = "com.mushroom.notificationlogger",
            fallbackAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        ) { action, pkg ->
            fallbackCalls.add(action to pkg)
            action == Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        assertTrue(fallbackOk)
        assertEquals(
            listOf(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS to
                    "com.mushroom.notificationlogger",
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS to null
            ),
            fallbackCalls
        )
    }

    @Test
    fun `manifests do not declare INTERNET or POST_NOTIFICATIONS permissions`() {
        val mainManifest = sequenceOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).firstOrNull { it.isFile }
        assertTrue("src/main/AndroidManifest.xml should exist", mainManifest != null)
        val mainText = mainManifest!!.readText()
        assertFalse(mainText.contains("android.permission.INTERNET"))
        assertFalse(mainText.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(mainText.contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"))

        val mergedManifest = sequenceOf(
            File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
        ).firstOrNull { it.isFile }
        if (mergedManifest != null) {
            val mergedText = mergedManifest.readText()
            assertFalse(mergedText.contains("android.permission.INTERNET"))
            assertFalse(mergedText.contains("android.permission.POST_NOTIFICATIONS"))
        }
    }
}
