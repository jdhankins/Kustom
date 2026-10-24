package com.hank.kustom.data.model

/**
 * Global typography configuration for all Kustom data fields.
 */
data class GlobalTypographyConfig(
    val fontFamily: KustomFont = KustomFont.BARLOW,
    val labelSizeSp: Int = 14,
    val labelWeight: Int = 700,
    val valueSizeSp: Int = 38,
    val valueWeight: Int = 700,
)
