package com.hank.kustom.data.model

/**
 * Typography styling parameters passed to [com.hank.kustom.domain.KustomFieldRenderer].
 *
 * @property font Selected [KustomFont] family option.
 * @property fontWeight Font weight (e.g. 300, 400, 500, 700, 900).
 * @property fontSizeSp Base font size in SP.
 */
data class TypographyConfig(
    /** Selected [KustomFont] option. */
    val font: KustomFont = KustomFont.KAROO,

    /** Font weight value. */
    val fontWeight: Int = 700,

    /** Base font size in SP. */
    val fontSizeSp: Float = 38f,
)
