package com.hank.kustom.data.model

/**
 * Global settings data structure combining global typography preferences and per-field configurations.
 *
 * @property globalTypography Shared typography configuration ([GlobalTypographyConfig]).
 * @property fieldConfigs Map of field typeId to its specific configuration ([KustomFieldConfig]).
 */
data class KustomSettings(
    /** Shared typography configuration. */
    val globalTypography: GlobalTypographyConfig = GlobalTypographyConfig(),

    /** Per-field configuration map keyed by [typeId]. */
    val fieldConfigs: Map<String, KustomFieldConfig> = emptyMap(),
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
