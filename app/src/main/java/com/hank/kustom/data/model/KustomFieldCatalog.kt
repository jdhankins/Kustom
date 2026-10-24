package com.hank.kustom.data.model

import io.hammerhead.karooext.models.DataType
import java.util.Locale

enum class FieldCategory(val displayName: String) {
    POWER("Power"),
    HEART_RATE("Heart Rate"),
    SPEED("Speed"),
    CADENCE("Cadence"),
    CLIMB("Climb"),
    TIME("Time"),
}

data class KustomFieldDefinition(
    val typeId: String,
    val childTitle: String,
    val category: FieldCategory,
    val description: String,
    val supportsSmoothing: Boolean,
    val supportsZones: Boolean,
    val mockValues: List<Double>,
) {
    val title: String
        get() = childTitle.uppercase()

    val displayName: String
        get() = pageSetupTitle

    val pageSetupTitle: String
        get() = "Kustom - $childTitle"

    fun resolveDynamicLabel(smoothing: SmoothingOption): String {
        return if (supportsSmoothing && smoothing != SmoothingOption.NONE) {
            "${smoothing.label} $childTitle"
        } else {
            childTitle
        }
    }

    fun formatNumericValueOnly(rawValue: Double, isImperial: Boolean = false): String {
        return when (typeId) {
            "speed", "avg-speed", "lap-speed" -> {
                val multiplier = if (isImperial) 2.23694 else 3.6
                val converted = rawValue * multiplier
                String.format(Locale.US, "%.1f", converted)
            }
            "distance" -> {
                val multiplier = if (isImperial) 0.621371 else 0.001
                val converted = rawValue * multiplier
                String.format(Locale.US, "%.1f", converted)
            }
            "grade" -> {
                String.format(Locale.US, "%.1f", rawValue)
            }
            "elevation", "ascent" -> {
                val multiplier = if (isImperial) 3.28084 else 1.0
                val converted = rawValue * multiplier
                converted.toInt().toString()
            }
            "ride-time" -> {
                val totalSec = rawValue.toInt()
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                if (hrs > 0) String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
                else String.format(Locale.US, "%02d:%02d", mins, secs)
            }
            else -> {
                rawValue.toInt().toString()
            }
        }
    }

    fun resolveSdkDataTypeId(smoothing: SmoothingOption): String {
        return when (typeId) {
            "power" -> smoothing.toPowerDataTypeId()
            "np" -> DataType.Type.NORMALIZED_POWER
            "avg-power" -> DataType.Type.AVERAGE_POWER
            "max-power" -> DataType.Type.MAX_POWER
            "lap-power" -> DataType.Type.POWER_LAP
            "last-lap-power" -> DataType.Type.AVERAGE_POWER_LAST_LAP
            "hr" -> DataType.Type.HEART_RATE
            "avg-hr" -> DataType.Type.AVERAGE_HR
            "max-hr" -> DataType.Type.MAX_HR
            "lap-hr" -> DataType.Type.AVERAGE_LAP_HR
            "speed" -> smoothing.toSpeedDataTypeId()
            "avg-speed" -> DataType.Type.AVERAGE_SPEED
            "lap-speed" -> DataType.Type.AVERAGE_SPEED_LAP
            "cadence" -> smoothing.toCadenceDataTypeId()
            "avg-cadence" -> DataType.Type.AVERAGE_CADENCE
            "grade" -> DataType.Type.ELEVATION_GRADE
            "vam" -> DataType.Type.AVERAGE_VERTICAL_SPEED_30S
            "distance" -> DataType.Type.DISTANCE
            "elevation" -> DataType.Type.PRESSURE_ELEVATION_CORRECTION
            "ascent" -> DataType.Type.ELEVATION_GAIN
            "ride-time" -> DataType.Type.ELAPSED_TIME
            else -> DataType.Type.POWER
        }
    }
}

object KustomFieldCatalog {

    val POWER = KustomFieldDefinition(
        typeId = "power",
        childTitle = "Power",
        category = FieldCategory.POWER,
        description = "Current power output",
        supportsSmoothing = true,
        supportsZones = true,
        mockValues = listOf(215.0, 228.0, 242.0, 260.0, 235.0),
    )

    val NORM_POWER = KustomFieldDefinition(
        typeId = "np",
        childTitle = "Norm Power",
        category = FieldCategory.POWER,
        description = "Normalized power output",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(220.0, 225.0, 230.0, 235.0, 228.0),
    )

    val AVG_POWER = KustomFieldDefinition(
        typeId = "avg-power",
        childTitle = "Avg Power",
        category = FieldCategory.POWER,
        description = "Average ride power",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(210.0, 215.0, 218.0, 220.0, 216.0),
    )

    val MAX_POWER = KustomFieldDefinition(
        typeId = "max-power",
        childTitle = "Max Power",
        category = FieldCategory.POWER,
        description = "Maximum ride power",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(450.0, 520.0, 680.0, 710.0, 690.0),
    )

    val LAP_POWER = KustomFieldDefinition(
        typeId = "lap-power",
        childTitle = "Lap Power",
        category = FieldCategory.POWER,
        description = "Average power this lap",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(220.0, 225.0, 228.0, 232.0, 226.0),
    )

    val LAST_LAP_POWER = KustomFieldDefinition(
        typeId = "last-lap-power",
        childTitle = "Last Lap Power",
        category = FieldCategory.POWER,
        description = "Average power previous lap",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(215.0, 218.0, 222.0, 225.0, 220.0),
    )

    val HR = KustomFieldDefinition(
        typeId = "hr",
        childTitle = "Heart Rate",
        category = FieldCategory.HEART_RATE,
        description = "Current heart rate",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(145.0, 148.0, 152.0, 156.0, 150.0),
    )

    val AVG_HR = KustomFieldDefinition(
        typeId = "avg-hr",
        childTitle = "Avg HR",
        category = FieldCategory.HEART_RATE,
        description = "Average ride heart rate",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(140.0, 142.0, 145.0, 147.0, 144.0),
    )

    val MAX_HR = KustomFieldDefinition(
        typeId = "max-hr",
        childTitle = "Max HR",
        category = FieldCategory.HEART_RATE,
        description = "Maximum ride heart rate",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(172.0, 175.0, 178.0, 180.0, 177.0),
    )

    val LAP_HR = KustomFieldDefinition(
        typeId = "lap-hr",
        childTitle = "Lap HR",
        category = FieldCategory.HEART_RATE,
        description = "Average heart rate this lap",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(146.0, 149.0, 151.0, 153.0, 150.0),
    )

    val SPEED = KustomFieldDefinition(
        typeId = "speed",
        childTitle = "Speed",
        category = FieldCategory.SPEED,
        description = "Current riding speed",
        supportsSmoothing = true,
        supportsZones = false,
        mockValues = listOf(8.4, 8.8, 9.2, 9.5, 9.0),
    )

    val AVG_SPEED = KustomFieldDefinition(
        typeId = "avg-speed",
        childTitle = "Avg Speed",
        category = FieldCategory.SPEED,
        description = "Average ride speed",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(8.1, 8.3, 8.5, 8.6, 8.4),
    )

    val LAP_SPEED = KustomFieldDefinition(
        typeId = "lap-speed",
        childTitle = "Lap Speed",
        category = FieldCategory.SPEED,
        description = "Average speed this lap",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(8.5, 8.7, 8.9, 9.1, 8.8),
    )

    val CADENCE = KustomFieldDefinition(
        typeId = "cadence",
        childTitle = "Cadence",
        category = FieldCategory.CADENCE,
        description = "Current pedaling cadence",
        supportsSmoothing = true,
        supportsZones = false,
        mockValues = listOf(85.0, 88.0, 92.0, 95.0, 90.0),
    )

    val AVG_CADENCE = KustomFieldDefinition(
        typeId = "avg-cadence",
        childTitle = "Avg Cadence",
        category = FieldCategory.CADENCE,
        description = "Average ride cadence",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(84.0, 86.0, 88.0, 89.0, 87.0),
    )

    val GRADE = KustomFieldDefinition(
        typeId = "grade",
        childTitle = "Grade",
        category = FieldCategory.CLIMB,
        description = "Current road gradient",
        supportsSmoothing = true,
        supportsZones = true,
        mockValues = listOf(4.2, 5.5, 6.8, 7.4, 5.1),
    )

    val VAM = KustomFieldDefinition(
        typeId = "vam",
        childTitle = "VAM",
        category = FieldCategory.CLIMB,
        description = "Vertical ascent speed",
        supportsSmoothing = true,
        supportsZones = false,
        mockValues = listOf(850.0, 920.0, 1050.0, 1100.0, 980.0),
    )

    val DISTANCE = KustomFieldDefinition(
        typeId = "distance",
        childTitle = "Distance",
        category = FieldCategory.SPEED,
        description = "Distance traveled",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(12400.0, 12800.0, 13200.0, 13600.0, 14100.0),
    )

    val ELEVATION = KustomFieldDefinition(
        typeId = "elevation",
        childTitle = "Elevation",
        category = FieldCategory.CLIMB,
        description = "Current altitude",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(320.0, 325.0, 332.0, 340.0, 345.0),
    )

    val ASCENT = KustomFieldDefinition(
        typeId = "ascent",
        childTitle = "Ascent",
        category = FieldCategory.CLIMB,
        description = "Total elevation gained",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(450.0, 455.0, 462.0, 470.0, 475.0),
    )

    val RIDE_TIME = KustomFieldDefinition(
        typeId = "ride-time",
        childTitle = "Ride Time",
        category = FieldCategory.TIME,
        description = "Elapsed ride time",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(3600.0, 3605.0, 3610.0, 3615.0, 3620.0),
    )

    val allFields: List<KustomFieldDefinition> = listOf(
        POWER, NORM_POWER, AVG_POWER, MAX_POWER, LAP_POWER, LAST_LAP_POWER,
        HR, AVG_HR, MAX_HR, LAP_HR,
        SPEED, AVG_SPEED, LAP_SPEED, DISTANCE,
        CADENCE, AVG_CADENCE,
        GRADE, VAM, ELEVATION, ASCENT,
        RIDE_TIME,
    )

    val configurableFields: List<KustomFieldDefinition> = allFields.filter {
        it.supportsSmoothing || it.supportsZones
    }

    fun findByTypeId(typeId: String): KustomFieldDefinition? {
        return allFields.firstOrNull { it.typeId == typeId }
    }
}
