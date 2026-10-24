package com.hank.kustom.data.model

/**
 * Holds global typography preferences applied across all Kustom data fields.
 *
 * @property fontFamily The selected [KustomFont] family option (e.g. Karoo, Wahoo, Garmin, Athletic, Clean).
 * @property labelSizeSp Base font size in SP for top field labels (e.g. 14 to 30).
 * @property labelWeight Font weight for top field labels (e.g. 300, 400, 500, 700, 900).
 * @property valueSizeSp Base font size in SP for main numeric values (e.g. 40 to 72).
 * @property valueWeight Font weight for main numeric values (e.g. 300, 400, 500, 700, 900).
 */
data class GlobalTypographyConfig(
    /** The global [KustomFont] family option. */
    val fontFamily: KustomFont = KustomFont.KAROO_RBNO2,

    /** Base font size in SP for top field labels. */
    val labelSizeSp: Int = 18,

    /** Font weight for top field labels. */
    val labelWeight: Int = 700,

    /** Base font size in SP for main numeric values. */
    val valueSizeSp: Int = 48,

    /** Font weight for main numeric values. */
    val valueWeight: Int = 700,
)
