package com.hank.kustom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.repository.KustomSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [AndroidViewModel] exposing [settingsState] and update actions for Kustom UI.
 */
class KustomSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KustomSettingsRepository(application)

    val settingsState: StateFlow<KustomSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KustomSettings(),
        )

    fun updateGlobalTypography(typography: GlobalTypographyConfig) {
        viewModelScope.launch {
            repository.updateGlobalTypography(typography)
        }
    }

    fun updateFieldConfig(typeId: String, config: KustomFieldConfig) {
        viewModelScope.launch {
            repository.updateFieldConfig(typeId, config)
        }
    }
}
