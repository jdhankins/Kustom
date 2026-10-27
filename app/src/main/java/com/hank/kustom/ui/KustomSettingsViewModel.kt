package com.hank.kustom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hank.kustom.data.model.CustomComboInstance
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFont
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.data.model.ZonePalette
import com.hank.kustom.data.repository.KustomSettingsRepository
import com.hank.kustom.util.KustomLogger
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
                if (saveTypographyJob?.isActive != true) {
                    _settingsState.value = diskSettings
                }
            }
        }
    }

    /** Updates global font family atomically. */
    fun updateFontFamily(fontFamily: KustomFont) {
        val current = _settingsState.value
        val updatedTypo = current.globalTypography.copy(fontFamily = fontFamily)
        _settingsState.value = current.copy(globalTypography = updatedTypo)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateFontFamily(fontFamily)
        }
    }

    /** Updates value font size in SP atomically. */
    fun updateValueFontSizeSp(sizeSp: Int) {
        val current = _settingsState.value
        val updatedTypo = current.globalTypography.copy(valueSizeSp = sizeSp)
        _settingsState.value = current.copy(globalTypography = updatedTypo)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateValueFontSizeSp(sizeSp)
        }
    }

    /** Updates value font weight atomically. */
    fun updateValueFontWeight(weight: Int) {
        val current = _settingsState.value
        val updatedTypo = current.globalTypography.copy(valueWeight = weight)
        _settingsState.value = current.copy(globalTypography = updatedTypo)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateValueFontWeight(weight)
        }
    }

    /** Updates label font size in SP atomically. */
    fun updateLabelFontSizeSp(sizeSp: Int) {
        val current = _settingsState.value
        val updatedTypo = current.globalTypography.copy(labelSizeSp = sizeSp)
        _settingsState.value = current.copy(globalTypography = updatedTypo)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateLabelFontSizeSp(sizeSp)
        }
    }

    /** Updates label font weight atomically. */
    fun updateLabelFontWeight(weight: Int) {
        val current = _settingsState.value
        val updatedTypo = current.globalTypography.copy(labelWeight = weight)
        _settingsState.value = current.copy(globalTypography = updatedTypo)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateLabelFontWeight(weight)
        }
    }

    /** Updates Light Mode background color. */
    fun updateLightBgColor(hex: String) {
        val current = _settingsState.value
        val updatedColors = current.globalColors.copy(lightBgColorHex = hex, useCustomBg = true)
        _settingsState.value = current.copy(globalColors = updatedColors)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateLightBgColor(hex)
        }
    }

    /** Updates Dark Mode background color. */
    fun updateDarkBgColor(hex: String) {
        val current = _settingsState.value
        val updatedColors = current.globalColors.copy(darkBgColorHex = hex, useCustomBg = true)
        _settingsState.value = current.copy(globalColors = updatedColors)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateDarkBgColor(hex)
        }
    }

    /** Updates global default power zone count (7 or 5 zones). */
    fun updateGlobalPowerZoneCount(count: Int) {
        val current = _settingsState.value
        val updatedColors = current.globalColors.copy(globalPowerZoneCount = count)
        _settingsState.value = current.copy(globalColors = updatedColors)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateGlobalPowerZoneCount(count)
        }
    }

    /** Updates global zone color palette (Karoo, Garmin, Wahoo, Zwift). */
    fun updateGlobalZonePalette(palette: ZonePalette) {
        val current = _settingsState.value
        val updatedColors = current.globalColors.copy(globalZonePalette = palette)
        _settingsState.value = current.copy(globalColors = updatedColors)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateGlobalZonePalette(palette)
        }
    }

    /** Updates whether custom field background color is enabled. */
    fun updateUseCustomBg(enabled: Boolean) {
        val current = _settingsState.value
        val updatedColors = current.globalColors.copy(useCustomBg = enabled)
        _settingsState.value = current.copy(globalColors = updatedColors)

        saveTypographyJob?.cancel()
        saveTypographyJob = viewModelScope.launch {
            delay(300L)
            repository.updateUseCustomBg(enabled)
        }
    }

    /** Updates field smoothing option atomically without touching color mode. */
    fun updateFieldSmoothing(typeId: String, smoothing: SmoothingOption) {
        val current = _settingsState.value
        val updatedConfigs = current.fieldConfigs.toMutableMap()
        val currentConfig = updatedConfigs[typeId] ?: KustomFieldConfig()
        updatedConfigs[typeId] = currentConfig.copy(smoothingOption = smoothing)
        _settingsState.value = current.copy(fieldConfigs = updatedConfigs)

        saveFieldJobs[typeId]?.cancel()
        saveFieldJobs[typeId] = viewModelScope.launch {
            delay(300L)
            repository.updateFieldSmoothing(typeId, smoothing)
        }
    }

    /** Updates field zone format atomically. */
    fun updateFieldZoneFormat(typeId: String, zoneFormat: ZoneFormat) {
        val current = _settingsState.value
        val updatedConfigs = current.fieldConfigs.toMutableMap()
        val currentConfig = updatedConfigs[typeId] ?: KustomFieldConfig()
        updatedConfigs[typeId] = currentConfig.copy(zoneFormat = zoneFormat)
        _settingsState.value = current.copy(fieldConfigs = updatedConfigs)

        saveFieldJobs[typeId]?.cancel()
        saveFieldJobs[typeId] = viewModelScope.launch {
            delay(300L)
            repository.updateFieldZoneFormat(typeId, zoneFormat)
        }
    }

    /** Adds a new user-created dynamic 3-in-1 combo block instance. */
    fun addCustomComboInstance(name: String) {
        val current = _settingsState.value
        val nextNum = current.customComboInstances.size + 1
        val defaultBlockName = "Custom $nextNum"
        val newInstance = CustomComboInstance(
            id = "combo_${System.currentTimeMillis()}",
            name = name.ifBlank { defaultBlockName },
        )
        val updatedList = current.customComboInstances + newInstance
        _settingsState.value = current.copy(customComboInstances = updatedList)

        viewModelScope.launch {
            repository.saveCustomComboInstances(updatedList)
            restartExtensionService()
        }
    }

    /** Updates properties of an existing user-created dynamic 3-in-1 combo block instance. */
    fun updateCustomComboInstance(instance: CustomComboInstance) {
        val current = _settingsState.value
        val updatedList = current.customComboInstances.map {
            if (it.id == instance.id) instance else it
        }
        _settingsState.value = current.copy(customComboInstances = updatedList)

        viewModelScope.launch {
            repository.saveCustomComboInstances(updatedList)
            restartExtensionService()
        }
    }

    /** Deletes a user-created dynamic 3-in-1 combo block instance by id. */
    fun deleteCustomComboInstance(instanceId: String) {
        val current = _settingsState.value
        val updatedList = current.customComboInstances.filter { it.id != instanceId }
        _settingsState.value = current.copy(customComboInstances = updatedList)

        viewModelScope.launch {
            repository.saveCustomComboInstances(updatedList)
            restartExtensionService()
        }
    }

    private fun restartExtensionService() {
        runCatching {
            val app = getApplication<Application>()
            val intent = android.content.Intent(app, com.hank.kustom.extension.KustomExtension::class.java)
            app.stopService(intent)
            app.startService(intent)
        }.onFailure { throwable ->
            KustomLogger.e("Failed to restart KustomExtension service", throwable)
        }
    }
}
