package com.hank.kustom.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFont
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZoneFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kustom_settings")

/**
 * DataStore repository managing [KustomSettings] persistent user preferences.
 */
class KustomSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /**
     * [Flow] emitting updated [KustomSettings] whenever preferences are modified.
     */
    val settingsFlow: Flow<KustomSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val globalTypo = GlobalTypographyConfig(
                fontFamily = parseEnum(prefs[Keys.GLOBAL_FONT_FAMILY], KustomFont.BARLOW),
                labelSizeSp = prefs[Keys.GLOBAL_LABEL_SIZE_SP] ?: 14,
                labelWeight = prefs[Keys.GLOBAL_LABEL_WEIGHT] ?: 700,
                valueSizeSp = prefs[Keys.GLOBAL_VALUE_SIZE_SP] ?: 38,
                valueWeight = prefs[Keys.GLOBAL_VALUE_WEIGHT] ?: 700,
            )

            val configs = mutableMapOf<String, KustomFieldConfig>()
            KustomFieldCatalog.allFields.forEach { fieldDef ->
                val typeId = fieldDef.typeId
                val smoothingSec = prefs[intPreferencesKey("${typeId}_smoothing")] ?: 3
                val zoneFormatRaw = prefs[stringPreferencesKey("${typeId}_zone_format")]
                val showUnit = prefs[booleanPreferencesKey("${typeId}_show_unit")] ?: true

                val fieldConfig = KustomFieldConfig(
                    smoothingOption = SmoothingOption.fromSeconds(smoothingSec),
                    zoneFormat = parseEnum(zoneFormatRaw, if (fieldDef.supportsZones) ZoneFormat.BACKGROUND else ZoneFormat.NONE),
                    showUnit = showUnit,
                )
                configs[typeId] = fieldConfig
            }

            KustomSettings(
                globalTypography = globalTypo,
                fieldConfigs = configs,
            )
        }

    /**
     * Updates global typography.
     */
    suspend fun updateGlobalTypography(typography: GlobalTypographyConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_FONT_FAMILY] = typography.fontFamily.name
            prefs[Keys.GLOBAL_LABEL_SIZE_SP] = typography.labelSizeSp
            prefs[Keys.GLOBAL_LABEL_WEIGHT] = typography.labelWeight
            prefs[Keys.GLOBAL_VALUE_SIZE_SP] = typography.valueSizeSp
            prefs[Keys.GLOBAL_VALUE_WEIGHT] = typography.valueWeight
        }
    }

    /**
     * Updates configuration for a specific field by [typeId].
     */
    suspend fun updateFieldConfig(typeId: String, config: KustomFieldConfig) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("${typeId}_smoothing")] = config.smoothingOption.seconds
            prefs[stringPreferencesKey("${typeId}_zone_format")] = config.zoneFormat.name
            prefs[booleanPreferencesKey("${typeId}_show_unit")] = config.showUnit
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(savedString: String?, defaultValue: T): T {
        if (savedString.isNullOrBlank()) return defaultValue
        return runCatching { java.lang.Enum.valueOf(T::class.java, savedString) }.getOrDefault(defaultValue)
    }

    private object Keys {
        val GLOBAL_FONT_FAMILY = stringPreferencesKey("global_font_family")
        val GLOBAL_LABEL_SIZE_SP = intPreferencesKey("global_label_size_sp")
        val GLOBAL_LABEL_WEIGHT = intPreferencesKey("global_label_weight")
        val GLOBAL_VALUE_SIZE_SP = intPreferencesKey("global_value_size_sp")
        val GLOBAL_VALUE_WEIGHT = intPreferencesKey("global_value_weight")
    }
}
