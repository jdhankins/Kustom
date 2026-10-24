package com.hank.kustom.data.model

import io.hammerhead.karooext.models.DataType

/**
 * Supported smoothing options for data fields.
 */
enum class SmoothingOption(val seconds: Int, val label: String) {
    NONE(0, "0s"),
    THREE_SEC(3, "3s"),
    FIVE_SEC(5, "5s"),
    TEN_SEC(10, "10s"),
    THIRTY_SEC(30, "30s");

    companion object {
        fun fromSeconds(seconds: Int): SmoothingOption {
            return entries.firstOrNull { it.seconds == seconds } ?: THREE_SEC
        }
    }
}

/**
 * Maps [SmoothingOption] directly to the built-in [DataType.Type] power constants.
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
