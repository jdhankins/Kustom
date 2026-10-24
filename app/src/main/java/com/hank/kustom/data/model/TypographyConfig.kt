package com.hank.kustom.data.model

/**
 * Typography styling configuration for a data field component.
 *
 * @property font [KustomFont] enum choice
 * @property fontWeight Font weight (e.g. 300, 400, 500, 700, 900)
 * @property fontSizeSp Base font size in SP
 */
data class TypographyConfig(
    val font: KustomFont = KustomFont.KAROO_RBNO2,
    val fontWeight: Int = 700,
    val fontSizeSp: Float = 38f,
)
