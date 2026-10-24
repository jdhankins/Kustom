package com.hank.kustom.data.model

/**
 * Global settings model holding global typography and per-field configs keyed by field [typeId].
 */
data class KustomSettings(
    val globalTypography: GlobalTypographyConfig = GlobalTypographyConfig(),
    val fieldConfigs: Map<String, KustomFieldConfig> = emptyMap(),
) {
    fun getFieldConfig(typeId: String): KustomFieldConfig {
        return fieldConfigs[typeId] ?: KustomFieldConfig()
    }
}
