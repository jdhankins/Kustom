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
import com.hank.kustom.data.model.CustomComboInstance
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFont
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.data.model.ZonePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
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
                globalPowerZoneCount = prefs[Keys.GLOBAL_POWER_ZONE_COUNT] ?: 7,
                globalZonePalette = parseEnum(prefs[Keys.GLOBAL_ZONE_PALETTE], ZonePalette.KAROO),
            )

            val configs = mutableMapOf<String, KustomFieldConfig>()
            KustomFieldCatalog.allFields.forEach { fieldDef ->
                val typeId = fieldDef.typeId
                val smoothingSec = prefs[intPreferencesKey("${typeId}_smoothing")] ?: 3
                val zoneFormatRaw = prefs[stringPreferencesKey("${typeId}_zone_format")]
                val showUnit = prefs[booleanPreferencesKey("${typeId}_show_unit")] ?: true
                val defaultZoneFormat = if (typeId == "power-combo" || typeId == "custom-combo") ZoneFormat.BAR else (if (fieldDef.supportsZones) ZoneFormat.BACKGROUND else ZoneFormat.NONE)
                val centerMetric = prefs[stringPreferencesKey("${typeId}_combo_center")] ?: "power"
                val leftMetric = prefs[stringPreferencesKey("${typeId}_combo_left")] ?: if (typeId == "power-combo" || typeId == "custom-combo") "np" else "avg-power"
                val rightMetric = prefs[stringPreferencesKey("${typeId}_combo_right")] ?: if (typeId == "power-combo" || typeId == "custom-combo") "max-power" else "np"
                val zoneCount = prefs[intPreferencesKey("${typeId}_combo_zones")] ?: 7

                val leftSmoothSec = prefs[intPreferencesKey("${typeId}_combo_left_smoothing")] ?: 0
                val rightSmoothSec = prefs[intPreferencesKey("${typeId}_combo_right_smoothing")] ?: 0
                val leftZoneRaw = prefs[stringPreferencesKey("${typeId}_combo_left_zone")]
                val rightZoneRaw = prefs[stringPreferencesKey("${typeId}_combo_right_zone")]

                val showPowerTrace = prefs[booleanPreferencesKey("${typeId}_show_power_trace")] ?: true
                val showHrTrace = prefs[booleanPreferencesKey("${typeId}_show_hr_trace")] ?: true
                val showCadenceTrace = prefs[booleanPreferencesKey("${typeId}_show_cadence_trace")] ?: true

                val fieldConfig = KustomFieldConfig(
                    smoothingOption = SmoothingOption.fromSeconds(smoothingSec),
                    zoneFormat = parseEnum(zoneFormatRaw, defaultZoneFormat),
                    showUnit = showUnit,
                    comboCenterMetric = centerMetric,
                    comboLeftMetric = leftMetric,
                    comboRightMetric = rightMetric,
                    comboLeftSmoothing = SmoothingOption.fromSeconds(leftSmoothSec),
                    comboRightSmoothing = SmoothingOption.fromSeconds(rightSmoothSec),
                    comboLeftZoneFormat = parseEnum(leftZoneRaw, ZoneFormat.NONE),
                    comboRightZoneFormat = parseEnum(rightZoneRaw, ZoneFormat.NONE),
                    comboZoneCount = zoneCount,
                    showPowerTrace = showPowerTrace,
                    showHrTrace = showHrTrace,
                    showCadenceTrace = showCadenceTrace,
                )
                configs[typeId] = fieldConfig
            }

            val customCombos = parseCustomComboInstances(prefs[Keys.CUSTOM_COMBO_INSTANCES_JSON])

            KustomSettings(
                globalTypography = globalTypo,
                globalColors = globalColors,
                fieldConfigs = configs,
                customComboInstances = customCombos,
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
     * Atomically updates label font size in SP.
     */
    suspend fun updateLabelFontSizeSp(sizeSp: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LABEL_SIZE_SP] = sizeSp
        }
    }

    /**
     * Atomically updates label font weight.
     */
    suspend fun updateLabelFontWeight(weight: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LABEL_WEIGHT] = weight
        }
    }

    /**
     * Atomically updates value font size in SP.
     */
    suspend fun updateValueFontSizeSp(sizeSp: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_VALUE_SIZE_SP] = sizeSp
        }
    }

    /**
     * Atomically updates value font weight.
     */
    suspend fun updateValueFontWeight(weight: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_VALUE_WEIGHT] = weight
        }
    }

    /**
     * Atomically updates Light Mode background color.
     */
    suspend fun updateLightBgColor(hex: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_LIGHT_BG_COLOR] = hex
            prefs[Keys.GLOBAL_USE_CUSTOM_BG] = true
        }
    }

    /**
     * Atomically updates Dark Mode background color.
     */
    suspend fun updateDarkBgColor(hex: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_DARK_BG_COLOR] = hex
            prefs[Keys.GLOBAL_USE_CUSTOM_BG] = true
        }
    }

    /**
     * Atomically updates global power zone count (7 or 5 zones) in DataStore.
     */
    suspend fun updateGlobalPowerZoneCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_POWER_ZONE_COUNT] = count
        }
    }

    /**
     * Atomically updates global zone color palette (Karoo, Garmin, Wahoo, Zwift) in DataStore.
     */
    suspend fun updateGlobalZonePalette(palette: ZonePalette) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_ZONE_PALETTE] = palette.name
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
     * Atomically updates field zone format in DataStore.
     */
    suspend fun updateFieldZoneFormat(typeId: String, zoneFormat: ZoneFormat) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${typeId}_zone_format")] = zoneFormat.name
        }
    }

    /** Updates Power trace line toggle for workout graph fields. */
    suspend fun updateShowPowerTrace(typeId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("${typeId}_show_power_trace")] = enabled
        }
    }

    /** Updates Heart Rate trace line toggle for workout graph fields. */
    suspend fun updateShowHrTrace(typeId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("${typeId}_show_hr_trace")] = enabled
        }
    }

    /** Updates Cadence trace line toggle for workout graph fields. */
    suspend fun updateShowCadenceTrace(typeId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("${typeId}_show_cadence_trace")] = enabled
        }
    }

    /**
     * Atomically updates user-created dynamic 3-in-1 combo block instances in DataStore.
     */
    suspend fun saveCustomComboInstances(instances: List<CustomComboInstance>) {
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_COMBO_INSTANCES_JSON] = serializeCustomComboInstances(instances)
        }
    }

    private fun parseCustomComboInstances(jsonStr: String?): List<CustomComboInstance> {
        val defaultInstances = listOf(
            CustomComboInstance(
                id = "combo-block-1",
                name = "Block 1",
                centerMetric = "power",
                leftMetric = "np",
                rightMetric = "max-power",
                centerZoneFormat = ZoneFormat.BAR,
                centerSmoothing = SmoothingOption.THREE_SEC,
            ),
            CustomComboInstance(
                id = "combo-block-2",
                name = "Block 2",
                centerMetric = "hr",
                leftMetric = "avg-hr",
                rightMetric = "max-hr",
                centerZoneFormat = ZoneFormat.BACKGROUND,
                centerSmoothing = SmoothingOption.NONE,
            ),
            CustomComboInstance(
                id = "combo-block-3",
                name = "Block 3",
                centerMetric = "grade",
                leftMetric = "vam",
                rightMetric = "ascent",
                centerZoneFormat = ZoneFormat.BACKGROUND,
                centerSmoothing = SmoothingOption.NONE,
            ),
            CustomComboInstance(
                id = "combo-block-4",
                name = "Block 4",
                centerMetric = "power",
                leftMetric = "avg-speed",
                rightMetric = "distance",
                centerZoneFormat = ZoneFormat.BAR,
                centerSmoothing = SmoothingOption.THREE_SEC,
            ),
            CustomComboInstance(
                id = "combo-block-5",
                name = "Block 5",
                centerMetric = "cadence",
                leftMetric = "avg-cadence",
                rightMetric = "max-cadence",
                centerZoneFormat = ZoneFormat.NONE,
                centerSmoothing = SmoothingOption.NONE,
            ),
        )

        if (jsonStr.isNullOrBlank()) return defaultInstances
        return runCatching {
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() == 0) return defaultInstances
            val list = mutableListOf<CustomComboInstance>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CustomComboInstance(
                        id = obj.optString("id", "combo-block-${i + 1}"),
                        name = obj.optString("name", "Custom ${i + 1}"),
                        centerMetric = obj.optString("centerMetric", "power"),
                        leftMetric = obj.optString("leftMetric", "np"),
                        rightMetric = obj.optString("rightMetric", "max-power"),
                        centerZoneFormat = parseEnum(obj.optString("centerZoneFormat"), ZoneFormat.BAR),
                        leftZoneFormat = parseEnum(obj.optString("leftZoneFormat"), ZoneFormat.NONE),
                        rightZoneFormat = parseEnum(obj.optString("rightZoneFormat"), ZoneFormat.NONE),
                        centerSmoothing = SmoothingOption.fromSeconds(obj.optInt("centerSmoothing", 3)),
                        leftSmoothing = SmoothingOption.fromSeconds(obj.optInt("leftSmoothing", 0)),
                        rightSmoothing = SmoothingOption.fromSeconds(obj.optInt("rightSmoothing", 0)),
                    )
                )
            }
            if (list.isEmpty()) defaultInstances else list
        }.getOrDefault(defaultInstances)
    }

    private fun serializeCustomComboInstances(instances: List<CustomComboInstance>): String {
        val jsonArray = JSONArray()
        instances.forEach { inst ->
            val obj = JSONObject().apply {
                put("id", inst.id)
                put("name", inst.name)
                put("centerMetric", inst.centerMetric)
                put("leftMetric", inst.leftMetric)
                put("rightMetric", inst.rightMetric)
                put("centerZoneFormat", inst.centerZoneFormat.name)
                put("leftZoneFormat", inst.leftZoneFormat.name)
                put("rightZoneFormat", inst.rightZoneFormat.name)
                put("centerSmoothing", inst.centerSmoothing.seconds)
                put("leftSmoothing", inst.leftSmoothing.seconds)
                put("rightSmoothing", inst.rightSmoothing.seconds)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
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
        val GLOBAL_POWER_ZONE_COUNT = intPreferencesKey("global_power_zone_count")
        val GLOBAL_ZONE_PALETTE = stringPreferencesKey("global_zone_palette")
        val CUSTOM_COMBO_INSTANCES_JSON = stringPreferencesKey("custom_combo_instances_json")
    }
}
