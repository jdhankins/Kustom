package com.hank.kustom.data.model

/**
 * Configuration options specific to an individual data field.
 *
 * @property smoothingOption Selected rolling smoothing duration ([SmoothingOption]).
 * @property zoneFormat Selected zone color formatting mode ([ZoneFormat]).
 * @property showUnit Whether unit label verbiage is enabled.
 * @property comboCenterMetric Center primary metric identifier for universal 3-in-1 combo fields.
 * @property comboLeftMetric Top-left corner metric identifier for combo block fields.
 * @property comboRightMetric Top-right corner metric identifier for combo block fields.
 * @property comboLeftSmoothing Independent rolling smoothing duration for left corner metric.
 * @property comboRightSmoothing Independent rolling smoothing duration for right corner metric.
 * @property comboLeftZoneFormat Independent zone color format for left corner metric.
 * @property comboRightZoneFormat Independent zone color format for right corner metric.
 * @property comboZoneCount Zone bar count (5 or 7 zones) for combo block fields.
 * @property showPowerTrace Whether live Power trace line (White) is enabled on workout graph fields.
 * @property showHrTrace Whether live Heart Rate trace line (Red) is enabled on workout graph fields.
 * @property showCadenceTrace Whether live Cadence trace line (Cyan) is enabled on workout graph fields.
 */
data class KustomFieldConfig(
    val smoothingOption: SmoothingOption = SmoothingOption.THREE_SEC,
    val zoneFormat: ZoneFormat = ZoneFormat.BAR,
    val showUnit: Boolean = true,
    val comboCenterMetric: String = "power",
    val comboLeftMetric: String = "np",
    val comboRightMetric: String = "max-power",
    val comboLeftSmoothing: SmoothingOption = SmoothingOption.NONE,
    val comboRightSmoothing: SmoothingOption = SmoothingOption.NONE,
    val comboLeftZoneFormat: ZoneFormat = ZoneFormat.NONE,
    val comboRightZoneFormat: ZoneFormat = ZoneFormat.NONE,
    val comboZoneCount: Int = 7,
    val showPowerTrace: Boolean = true,
    val showHrTrace: Boolean = true,
    val showCadenceTrace: Boolean = true,
) {
    val colorMode: ColorMode
        get() = zoneFormat.toColorMode()

    val smoothingSeconds: Int
        get() = smoothingOption.seconds
}
