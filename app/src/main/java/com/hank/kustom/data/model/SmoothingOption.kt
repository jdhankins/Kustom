package com.hank.kustom.data.model

import io.hammerhead.karooext.models.DataType

/**
 * Supported rolling smoothing duration options for data fields.
 *
 * @property seconds Duration in integer seconds.
 * @property label User-visible label string (e.g. "0s", "3s", "5s", "10s", "30s").
 */
enum class SmoothingOption(
    /** Duration in seconds. */
    val seconds: Int,

    /** User-visible label. */
    val label: String,
) {
    /** Instantaneous raw telemetry (0 seconds smoothing). */
    NONE(0, "0s"),

    /** 3-second rolling average smoothing. */
    THREE_SEC(3, "3s"),

    /** 5-second rolling average smoothing. */
    FIVE_SEC(5, "5s"),

    /** 10-second rolling average smoothing. */
    TEN_SEC(10, "10s"),

    /** 30-second rolling average smoothing. */
    THIRTY_SEC(30, "30s");

    companion object {
        /**
         * Resolves a [SmoothingOption] by its integer [seconds] value.
         *
         * @param seconds Duration in seconds (e.g. 0, 3, 5, 10, 30).
         * @return The matching [SmoothingOption], or [THREE_SEC] if unmapped.
         */
        fun fromSeconds(seconds: Int): SmoothingOption {
            return entries.firstOrNull { it.seconds == seconds } ?: THREE_SEC
        }
    }
}

/**
 * Maps [SmoothingOption] directly to the built-in [DataType.Type] power constants.
 *
 * @return The corresponding Karoo SDK power [DataType.Type] string.
 */
fun SmoothingOption.toPowerDataTypeId(): String {
    return when (this) {
        SmoothingOption.NONE -> DataType.Type.POWER
        SmoothingOption.THREE_SEC -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
        SmoothingOption.FIVE_SEC -> DataType.Type.SMOOTHED_5S_AVERAGE_POWER
        SmoothingOption.TEN_SEC -> DataType.Type.SMOOTHED_10S_AVERAGE_POWER
        SmoothingOption.THIRTY_SEC -> DataType.Type.SMOOTHED_30S_AVERAGE_POWER
    }
}

/**
 * Maps [SmoothingOption] directly to the built-in [DataType.Type] speed constants.
 *
 * @return The corresponding Karoo SDK speed [DataType.Type] string.
 */
fun SmoothingOption.toSpeedDataTypeId(): String {
    return when (this) {
        SmoothingOption.NONE -> DataType.Type.SPEED
        SmoothingOption.THREE_SEC -> DataType.Type.SMOOTHED_3S_AVERAGE_SPEED
        SmoothingOption.FIVE_SEC -> DataType.Type.SMOOTHED_5S_AVERAGE_SPEED
        SmoothingOption.TEN_SEC -> DataType.Type.SMOOTHED_10S_AVERAGE_SPEED
        SmoothingOption.THIRTY_SEC -> DataType.Type.SMOOTHED_10S_AVERAGE_SPEED
    }
}

/**
 * Maps [SmoothingOption] directly to the built-in [DataType.Type] cadence constants.
 *
 * @return The corresponding Karoo SDK cadence [DataType.Type] string.
 */
fun SmoothingOption.toCadenceDataTypeId(): String {
    return when (this) {
        SmoothingOption.NONE -> DataType.Type.CADENCE
        SmoothingOption.THREE_SEC -> DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE
        SmoothingOption.FIVE_SEC -> DataType.Type.SMOOTHED_5S_AVERAGE_CADENCE
        SmoothingOption.TEN_SEC -> DataType.Type.SMOOTHED_10S_AVERAGE_CADENCE
        SmoothingOption.THIRTY_SEC -> DataType.Type.SMOOTHED_10S_AVERAGE_CADENCE
    }
}
