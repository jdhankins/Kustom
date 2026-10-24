package com.hank.kustom.data.model

/**
 * Configuration for label and value typography.
 *
 * @property font [KustomFont] enum choice
 * @property fontWeight Weight integer (e.g. 300 Light, 400 Regular, 500 Medium, 700 Bold, 900 Black)
 * @property fontSizeSp Size in SP (e.g., default 14f for labels, 38f for values)
 */
data class TypographyConfig(
    val font: KustomFont = KustomFont.BARLOW,
    val fontWeight: Int = 400,
    val fontSizeSp: Float = 14f,
)
