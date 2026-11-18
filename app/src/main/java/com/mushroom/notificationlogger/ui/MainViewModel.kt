package com.mushroom.notificationlogger.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushroom.notificationlogger.NotificationLoggerApp
import com.mushroom.notificationlogger.data.local.AppSummary
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import com.mushroom.notificationlogger.data.preferences.HealthCheckRecord
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.data.preferences.UserPreferencesRepository
import com.mushroom.notificationlogger.data.repository.NotificationRepository
import com.mushroom.notificationlogger.export.NotificationExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val repository: NotificationRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackageName = MutableStateFlow<String?>(null)
    val selectedPackageName: StateFlow<String?> = _selectedPackageName.asStateFlow()

    private val _isLoadingList = MutableStateFlow(true)
    val isLoadingList: StateFlow<Boolean> = _isLoadingList.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationWithImages>> = combine(
        _selectedPackageName,
        _searchQuery.debounce { query -> if (query.isEmpty()) 0L else 150L }
    ) { pkg, query ->
        pkg to query
    }.flatMapLatest { (pkg, query) ->
        repository.observeNotifications(packageName = pkg, rawQuery = query)
    }.onEach {
        _isLoadingList.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val recordedApps: StateFlow<List<AppSummary>> = repository.observeRecordedApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = repository.observeTotalCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM
        )

    val excludeOngoing: StateFlow<Boolean> = preferencesRepository.excludeOngoingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val healthCheckRecord: StateFlow<HealthCheckRecord?> =
        preferencesRepository.healthCheckRecordFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSelectPackage(packageName: String?) {
        _selectedPackageName.value = packageName
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedPackageName.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setExcludeOngoing(exclude: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setExcludeOngoing(exclude)
        }
    }

    fun deleteNotification(id: Long, onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteNotification(id)
            onCompleted?.invoke()
        }
    }

    fun deleteAllNotifications(onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteAllNotifications()
            _selectedPackageName.value = null
            onCompleted?.invoke()
        }
    }

    fun exportJson(
        context: Context,
        onlyFiltered: Boolean,
        onIntentReady: (Intent) -> Unit,
        onMessage: (String) -> Unit
    ) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val records = if (onlyFiltered) {
                    notifications.value
                } else {
                    repository.getAllNotificationsForExport()
                }
                if (records.isEmpty()) {
                    onMessage("エクスポートできる通知記録がありません")
                    return@launch
                }
                val shareIntent = NotificationExporter.writeExportFileAndBuildShareIntent(
                    context = context.applicationContext,
                    records = records
                )
                onIntentReady(shareIntent)
            } catch (e: Exception) {
                onMessage("エクスポートに失敗しました: ${e.localizedMessage ?: "不明なエラー"}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun resolveImageFile(fileName: String): File {
        return repository.resolveImageFile(fileName)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as NotificationLoggerApp)
                MainViewModel(
                    repository = app.notificationRepository,
                    preferencesRepository = app.preferencesRepository
                )
            }
        }
    }
}
