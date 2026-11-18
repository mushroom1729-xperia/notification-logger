package com.mushroom.notificationlogger.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_EXCLUDE_ONGOING = booleanPreferencesKey("exclude_ongoing_notifications")
    }
}
