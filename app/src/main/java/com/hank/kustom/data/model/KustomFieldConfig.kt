package com.hank.kustom.data.model

/**
 * Configuration options specific to an individual data field.
 *
 * @property smoothingOption Selected rolling smoothing duration ([SmoothingOption]).
 * @property zoneFormat Selected zone color formatting mode ([ZoneFormat]).
 * @property showUnit Whether unit label verbiage is enabled.
 * @property comboLeftMetric Top-left corner metric identifier for combo block fields.
 * @property comboRightMetric Top-right corner metric identifier for combo block fields.
 * @property comboZoneCount Zone bar count (5 or 7 zones) for combo block fields.
 */
data class KustomFieldConfig(
    val smoothingOption: SmoothingOption = SmoothingOption.THREE_SEC,
    val zoneFormat: ZoneFormat = ZoneFormat.BAR,
    val showUnit: Boolean = true,
    val comboLeftMetric: String = "np",
    val comboRightMetric: String = "max-power",
    val comboZoneCount: Int = 7,
) {
    val colorMode: ColorMode
        get() = zoneFormat.toColorMode()

    val smoothingSeconds: Int
        get() = smoothingOption.seconds
}
