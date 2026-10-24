package com.hank.kustom.data.model

/**
 * Global typography configuration for all Kustom data fields.
 */
data class GlobalTypographyConfig(
    val fontFamily: KustomFont = KustomFont.KAROO_RBNO2,
    val labelSizeSp: Int = 18,
    val labelWeight: Int = 700,
    val valueSizeSp: Int = 48,
    val valueWeight: Int = 700,
)
