package com.hank.kustom.data.model

/**
 * Global settings data structure combining global typography, custom background colors, per-field configurations, and user-created 3-in-1 combo blocks.
 *
 * @property globalTypography Shared typography configuration ([GlobalTypographyConfig]).
 * @property globalColors Shared background color configuration ([GlobalColorConfig]).
 * @property fieldConfigs Map of field typeId to its specific configuration ([KustomFieldConfig]).
 * @property customComboInstances List of user-created dynamic 3-in-1 combo blocks ([CustomComboInstance]).
 */
data class KustomSettings(
    /** Shared typography configuration. */
    val globalTypography: GlobalTypographyConfig = GlobalTypographyConfig(),

    /** Shared background color configuration. */
    val globalColors: GlobalColorConfig = GlobalColorConfig(),

    /** Per-field configuration map keyed by [typeId]. */
    val fieldConfigs: Map<String, KustomFieldConfig> = emptyMap(),

    /** List of user-created dynamic 3-in-1 combo blocks. */
    val customComboInstances: List<CustomComboInstance> = emptyList(),
) {
    /**
     * Resolves the [KustomFieldConfig] for the specified [typeId], or returns a default configuration if missing.
     *
     * @param typeId The unique data type identifier (e.g. "power", "hr", "speed").
     * @return The corresponding [KustomFieldConfig] for [typeId].
     */
    fun getFieldConfig(typeId: String): KustomFieldConfig {
        return fieldConfigs[typeId] ?: KustomFieldConfig()
    }
}
