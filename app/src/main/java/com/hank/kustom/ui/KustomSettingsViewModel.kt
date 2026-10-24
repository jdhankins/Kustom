package com.hank.kustom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.repository.KustomSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * High-performance [AndroidViewModel] providing instant in-memory UI state updates
 * and debounced background DataStore persistence for butter-smooth 60fps sliders.
 */
class KustomSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KustomSettingsRepository(application)

    private val _settingsState = MutableStateFlow(KustomSettings())
    val settingsState: StateFlow<KustomSettings> = _settingsState.asStateFlow()

    private var saveTypographyJob: Job? = null
    private val saveFieldJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { diskSettings ->
                // Initial load or background disk sync
                if (_settingsState.value == KustomSettings()) {
                    _settingsState.value = diskSettings
                } else {
                    // Update field configs from disk without overwriting active dragging
                    _settingsState.value = _settingsState.value.copy(
                        fieldConfigs = diskSettings.fieldConfigs
                    )
                }
            }
        }
    }

    /**
     * Updates global typography instantly in memory for 0ms lag,
     * and debounces DataStore disk persistence by 300ms.
     */
    fun updateGlobalTypography(typography: GlobalTypographyConfig) {
        val current = _settingsState.value
        if (current.globalTypography == typography) return

        // 1. Instant 0ms In-Memory Main Thread Update
        _settingsState.value = current.copy(globalTypography = typography)

        // 2. Asynchronous Debounced Disk Persistence
        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateGlobalTypography(typography)
        }
    }

    /**
     * Updates field config instantly in memory for 0ms lag,
     * and debounces DataStore disk persistence by 300ms.
     */
    fun updateFieldConfig(typeId: String, config: KustomFieldConfig) {
        val current = _settingsState.value
        val updatedConfigs = current.fieldConfigs.toMutableMap()
        if (updatedConfigs[typeId] == config) return

        // 1. Instant 0ms In-Memory Main Thread Update
        updatedConfigs[typeId] = config
        _settingsState.value = current.copy(fieldConfigs = updatedConfigs)

        // 2. Asynchronous Debounced Disk Persistence
        saveFieldJobs[typeId]?.cancel()
        saveFieldJobs[typeId] = viewModelScope.launch {
            delay(300L)
            repository.updateFieldConfig(typeId, config)
        }
    }
}
