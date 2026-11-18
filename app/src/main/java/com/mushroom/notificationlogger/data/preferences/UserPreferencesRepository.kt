package com.mushroom.notificationlogger.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val storageKey: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromStorageKey(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == value } ?: SYSTEM
        }
    }
}

data class HealthCheckRecord(
    val checkedAtMillis: Long,
    val wasConnected: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val isBackgroundRestricted: Boolean,
    val rebindRequested: Boolean
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_logger_prefs"
)

class UserPreferencesRepository(private val context: Context) {

    private val safeDataFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val themeModeFlow: Flow<ThemeMode> = safeDataFlow.map { prefs ->
        ThemeMode.fromStorageKey(prefs[KEY_THEME_MODE])
    }

    val excludeOngoingFlow: Flow<Boolean> = safeDataFlow.map { prefs ->
        prefs[KEY_EXCLUDE_ONGOING] ?: true
    }

    val healthCheckRecordFlow: Flow<HealthCheckRecord?> = safeDataFlow.map { prefs ->
        val checkedAt = prefs[KEY_LAST_HEALTH_CHECK_AT] ?: return@map null
        HealthCheckRecord(
            checkedAtMillis = checkedAt,
            wasConnected = prefs[KEY_LAST_CHECK_CONNECTED] ?: false,
            isIgnoringBatteryOptimizations = prefs[KEY_LAST_CHECK_IGNORING_BATTERY_OPT] ?: false,
            isBackgroundRestricted = prefs[KEY_LAST_CHECK_BG_RESTRICTED] ?: false,
            rebindRequested = prefs[KEY_LAST_CHECK_REBIND_REQUESTED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.storageKey
        }
    }

    suspend fun setExcludeOngoing(exclude: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXCLUDE_ONGOING] = exclude
        }
    }

    suspend fun recordHealthCheckResult(
        checkedAtMillis: Long,
        wasConnected: Boolean,
        isIgnoringBatteryOptimizations: Boolean,
        isBackgroundRestricted: Boolean,
        rebindRequested: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_HEALTH_CHECK_AT] = checkedAtMillis
            prefs[KEY_LAST_CHECK_CONNECTED] = wasConnected
            prefs[KEY_LAST_CHECK_IGNORING_BATTERY_OPT] = isIgnoringBatteryOptimizations
            prefs[KEY_LAST_CHECK_BG_RESTRICTED] = isBackgroundRestricted
            prefs[KEY_LAST_CHECK_REBIND_REQUESTED] = rebindRequested
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_EXCLUDE_ONGOING = booleanPreferencesKey("exclude_ongoing_notifications")
        private val KEY_LAST_HEALTH_CHECK_AT = longPreferencesKey("last_health_check_at")
        private val KEY_LAST_CHECK_CONNECTED = booleanPreferencesKey("last_health_check_connected")
        private val KEY_LAST_CHECK_IGNORING_BATTERY_OPT =
            booleanPreferencesKey("last_health_check_ignoring_battery_opt")
        private val KEY_LAST_CHECK_BG_RESTRICTED =
            booleanPreferencesKey("last_health_check_bg_restricted")
        private val KEY_LAST_CHECK_REBIND_REQUESTED =
            booleanPreferencesKey("last_health_check_rebind_requested")
    }
}
