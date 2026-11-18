package com.mushroom.notificationlogger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushroom.notificationlogger.NotificationLoggerApp
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import com.mushroom.notificationlogger.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Found(val record: NotificationWithImages) : DetailUiState
    data object NotFound : DetailUiState
}

class DetailViewModel(
    private val notificationId: Long,
    private val repository: NotificationRepository
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = repository.observeNotificationById(notificationId)
        .map { item ->
            if (item != null) DetailUiState.Found(item) else DetailUiState.NotFound
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading
        )

    fun deleteRecord(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
            onDeleted()
        }
    }

    fun resolveImageFile(fileName: String): File {
        return repository.resolveImageFile(fileName)
    }

    companion object {
        fun provideFactory(notificationId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as NotificationLoggerApp)
                DetailViewModel(
                    notificationId = notificationId,
                    repository = app.notificationRepository
                )
            }
        }
    }
}
