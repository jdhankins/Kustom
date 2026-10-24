package com.hank.kustom.data.model

/**
 * Field-specific configuration for smoothing, zone format, and unit rendering.
 */
data class KustomFieldConfig(
    val smoothingOption: SmoothingOption = SmoothingOption.THREE_SEC,
    val zoneFormat: ZoneFormat = ZoneFormat.NONE,
    val showUnit: Boolean = true,
) {
    val colorMode: ColorMode
        get() = zoneFormat.toColorMode()

    val smoothingSeconds: Int
        get() = smoothingOption.seconds
}
