package com.watchsafety.guardian.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.watchsafety.guardian.data.GuardianRepository
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GuardianUiState(
    val snapshot: GuardianSnapshot,
    val isRefreshing: Boolean = false,
)

class GuardianViewModel(
    private val repository: GuardianRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<GuardianUiState> = combine(
        repository.snapshot,
        isRefreshing,
    ) { snapshot, refreshing ->
        GuardianUiState(snapshot = snapshot, isRefreshing = refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GuardianUiState(repository.snapshot.value),
    )

    fun refreshStatus() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refreshStatus()
            isRefreshing.value = false
        }
    }

    fun sendReturnHomeRequest() {
        viewModelScope.launch { repository.sendReturnHomeRequest() }
    }

    fun setSafeZoneEnabled(zoneId: String, enabled: Boolean) {
        viewModelScope.launch { repository.setSafeZoneEnabled(zoneId, enabled) }
    }

    fun addSafeZone(name: String, radiusMeters: Int) {
        viewModelScope.launch { repository.addSafeZone(name, radiusMeters) }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch { repository.updateNotificationSettings(settings) }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GuardianViewModel::class.java))
                return GuardianViewModel(MockGuardianRepository()) as T
            }
        }
    }
}
