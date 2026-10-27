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
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
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
 * DataStore repository managing [KustomSettings] persistent user preferences with atomic per-property updates.
 *
 * @param context Application or activity context.
 */
class KustomSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /**
     * Reactive [Flow] emitting updated [KustomSettings] whenever user preferences are modified.
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
                fontFamily = parseEnum(prefs[Keys.GLOBAL_FONT_FAMILY], KustomFont.KAROO),
                labelSizeSp = prefs[Keys.GLOBAL_LABEL_SIZE_SP] ?: 18,
                labelWeight = prefs[Keys.GLOBAL_LABEL_WEIGHT] ?: 700,
                valueSizeSp = prefs[Keys.GLOBAL_VALUE_SIZE_SP] ?: 48,
                valueWeight = prefs[Keys.GLOBAL_VALUE_WEIGHT] ?: 700,
            )

            val globalColors = GlobalColorConfig(
                lightBgColorHex = prefs[Keys.GLOBAL_LIGHT_BG_COLOR] ?: "#FFFFFF",
                darkBgColorHex = prefs[Keys.GLOBAL_DARK_BG_COLOR] ?: "#000000",
                useCustomBg = prefs[Keys.GLOBAL_USE_CUSTOM_BG] ?: false,
            )

            val configs = mutableMapOf<String, KustomFieldConfig>()
            KustomFieldCatalog.allFields.forEach { fieldDef ->
                val typeId = fieldDef.typeId
                val smoothingSec = prefs[intPreferencesKey("${typeId}_smoothing")] ?: 3
                val zoneFormatRaw = prefs[stringPreferencesKey("${typeId}_zone_format")]
                val showUnit = prefs[booleanPreferencesKey("${typeId}_show_unit")] ?: true
                val leftMetric = prefs[stringPreferencesKey("${typeId}_combo_left")] ?: "avg-power"
                val rightMetric = prefs[stringPreferencesKey("${typeId}_combo_right")] ?: "np"
                val zoneCount = prefs[intPreferencesKey("${typeId}_combo_zones")] ?: 7

                val fieldConfig = KustomFieldConfig(
                    smoothingOption = SmoothingOption.fromSeconds(smoothingSec),
                    zoneFormat = parseEnum(zoneFormatRaw, if (fieldDef.supportsZones) ZoneFormat.BACKGROUND else ZoneFormat.NONE),
                    showUnit = showUnit,
                    comboLeftMetric = leftMetric,
                    comboRightMetric = rightMetric,
                    comboZoneCount = zoneCount,
                )
                configs[typeId] = fieldConfig
            }

            KustomSettings(
                globalTypography = globalTypo,
                globalColors = globalColors,
                fieldConfigs = configs,
            )
        }

    /**
     * Atomically updates font family in DataStore.
     */
    suspend fun updateFontFamily(fontFamily: KustomFont) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_FONT_FAMILY] = fontFamily.name
        }
    }

    /**
     * Atomically updates value font size in DataStore.
     */
    suspend fun updateValueFontSizeSp(sizeSp: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_VALUE_SIZE_SP] = sizeSp
        }
    }

    /**
     * Atomically updates value font weight in DataStore.
     */
    suspend fun updateValueFontWeight(weight: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_VALUE_WEIGHT] = weight
        }
    }

    /**
     * Atomically updates label font size in DataStore.
     */
    suspend fun updateLabelFontSizeSp(sizeSp: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LABEL_SIZE_SP] = sizeSp
        }
    }

    /**
     * Atomically updates label font weight in DataStore.
     */
    suspend fun updateLabelFontWeight(weight: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LABEL_WEIGHT] = weight
        }
    }

    /**
     * Atomically updates Light Mode background color in DataStore.
     */
    suspend fun updateLightBgColor(hex: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LIGHT_BG_COLOR] = hex
            prefs[Keys.GLOBAL_USE_CUSTOM_BG] = true
        }
    }

    /**
     * Atomically updates Dark Mode background color in DataStore.
     */
    suspend fun updateDarkBgColor(hex: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_DARK_BG_COLOR] = hex
            prefs[Keys.GLOBAL_USE_CUSTOM_BG] = true
        }
    }

    /**
     * Atomically updates field zone format in DataStore.
     */
    suspend fun updateFieldZoneFormat(typeId: String, zoneFormat: ZoneFormat) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${typeId}_zone_format")] = zoneFormat.name
        }
    }

    suspend fun updateComboLeftMetric(typeId: String, metric: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${typeId}_combo_left")] = metric
        }
    }

    suspend fun updateComboRightMetric(typeId: String, metric: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${typeId}_combo_right")] = metric
        }
    }

    suspend fun updateComboZoneCount(typeId: String, count: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("${typeId}_combo_zones")] = count
        }
    }

    /**
     * Atomically updates whether custom field background color is enabled.
     */
    suspend fun updateUseCustomBg(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_USE_CUSTOM_BG] = enabled
        }
    }

    /**
     * Atomically updates field smoothing in DataStore without touching zone color.
     */
    suspend fun updateFieldSmoothing(typeId: String, smoothing: SmoothingOption) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("${typeId}_smoothing")] = smoothing.seconds
        }
    }

    /**
     * Atomically updates field color mode / zone format in DataStore without touching smoothing.
     */
    suspend fun updateFieldColorMode(typeId: String, colorMode: ColorMode) {
        val zoneFormat = ZoneFormat.fromColorMode(colorMode)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${typeId}_zone_format")] = zoneFormat.name
        }
    }

    /**
     * Legacy helper updating full global typography.
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
     * Legacy helper updating full field config.
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
        val GLOBAL_LIGHT_BG_COLOR = stringPreferencesKey("global_light_bg_color")
        val GLOBAL_DARK_BG_COLOR = stringPreferencesKey("global_dark_bg_color")
        val GLOBAL_USE_CUSTOM_BG = booleanPreferencesKey("global_use_custom_bg")
    }
}
