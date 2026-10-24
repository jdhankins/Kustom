package com.hank.kustom.data.model

/**
 * Configuration options specific to an individual data field.
 *
 * @property smoothingOption Selected rolling smoothing duration ([SmoothingOption]).
 * @property zoneFormat Selected zone color formatting mode ([ZoneFormat]).
 * @property showUnit Whether unit label verbiage is enabled (kept false for numbers-only rendering).
 */
data class KustomFieldConfig(
    /** Selected rolling smoothing duration. */
    val smoothingOption: SmoothingOption = SmoothingOption.THREE_SEC,

    /** Selected zone color formatting mode. */
    val zoneFormat: ZoneFormat = ZoneFormat.NONE,

    /** Whether unit verbiage is displayed. */
    val showUnit: Boolean = true,
) {
    /**
     * Resolves the corresponding [ColorMode] from the active [zoneFormat].
     *
     * @return The active [ColorMode] (NONE, BACKGROUND_COLOR, or TEXT_COLOR).
     */
    val colorMode: ColorMode
        get() = zoneFormat.toColorMode()

    /**
     * Returns the rolling smoothing duration in integer seconds.
     *
     * @return The smoothing duration in seconds.
     */
    val smoothingSeconds: Int
        get() = smoothingOption.seconds
}
