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
            "speed", "avg-speed", "max-speed", "lap-speed" -> {
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
            "elevation", "ascent", "descent" -> {
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
            "clock-time" -> {
                "09:42"
            }
            "percent-ftp", "percent-max-hr" -> {
                "${rawValue.toInt()}"
            }
            "intensity-factor" -> {
                String.format(Locale.US, "%.2f", rawValue)
            }
            "power-to-weight" -> {
                String.format(Locale.US, "%.1f", rawValue)
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
            "percent-ftp" -> DataType.Type.PERCENT_MAX_FTP
            "intensity-factor" -> DataType.Type.INTENSITY_FACTOR
            "tss" -> DataType.Type.TRAINING_STRESS_SCORE
            "power-to-weight" -> DataType.Type.POWER_TO_WEIGHT
            "hr" -> DataType.Type.HEART_RATE
            "avg-hr" -> DataType.Type.AVERAGE_HR
            "max-hr" -> DataType.Type.MAX_HR
            "lap-hr" -> DataType.Type.AVERAGE_LAP_HR
            "percent-max-hr" -> DataType.Type.PERCENT_MAX_HR
            "speed" -> smoothing.toSpeedDataTypeId()
            "avg-speed" -> DataType.Type.AVERAGE_SPEED
            "max-speed" -> DataType.Type.MAX_SPEED
            "lap-speed" -> DataType.Type.AVERAGE_SPEED_LAP
            "distance" -> DataType.Type.DISTANCE
            "cadence" -> smoothing.toCadenceDataTypeId()
            "avg-cadence" -> DataType.Type.AVERAGE_CADENCE
            "max-cadence" -> DataType.Type.MAX_CADENCE
            "grade" -> DataType.Type.ELEVATION_GRADE
            "vam" -> DataType.Type.AVERAGE_VERTICAL_SPEED_30S
            "elevation" -> DataType.Type.PRESSURE_ELEVATION_CORRECTION
            "ascent" -> DataType.Type.ELEVATION_GAIN
            "descent" -> DataType.Type.ELEVATION_LOSS
            "ride-time" -> DataType.Type.ELAPSED_TIME
            "clock-time" -> DataType.Type.CLOCK_TIME
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

    val PERCENT_FTP = KustomFieldDefinition(
        typeId = "percent-ftp",
        childTitle = "% FTP",
        category = FieldCategory.POWER,
        description = "Percentage of threshold power",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(82.0, 85.0, 88.0, 92.0, 87.0),
    )

    val IF = KustomFieldDefinition(
        typeId = "intensity-factor",
        childTitle = "Intensity Factor",
        category = FieldCategory.POWER,
        description = "Ride intensity factor",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(0.82, 0.85, 0.88, 0.90, 0.86),
    )

    val TSS = KustomFieldDefinition(
        typeId = "tss",
        childTitle = "TSS",
        category = FieldCategory.POWER,
        description = "Training stress score",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(110.0, 115.0, 122.0, 130.0, 125.0),
    )

    val POWER_TO_WEIGHT = KustomFieldDefinition(
        typeId = "power-to-weight",
        childTitle = "Power/Weight",
        category = FieldCategory.POWER,
        description = "Power output in W/kg",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(3.2, 3.4, 3.6, 3.8, 3.5),
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

    val PERCENT_MAX_HR = KustomFieldDefinition(
        typeId = "percent-max-hr",
        childTitle = "% Max HR",
        category = FieldCategory.HEART_RATE,
        description = "Percentage of max heart rate",
        supportsSmoothing = false,
        supportsZones = true,
        mockValues = listOf(78.0, 80.0, 82.0, 85.0, 81.0),
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

    val MAX_SPEED = KustomFieldDefinition(
        typeId = "max-speed",
        childTitle = "Max Speed",
        category = FieldCategory.SPEED,
        description = "Maximum ride speed",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(12.5, 13.8, 14.2, 15.1, 14.0),
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

    val DISTANCE = KustomFieldDefinition(
        typeId = "distance",
        childTitle = "Distance",
        category = FieldCategory.SPEED,
        description = "Distance traveled",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(12400.0, 12800.0, 13200.0, 13600.0, 14100.0),
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

    val MAX_CADENCE = KustomFieldDefinition(
        typeId = "max-cadence",
        childTitle = "Max Cadence",
        category = FieldCategory.CADENCE,
        description = "Maximum ride cadence",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(115.0, 120.0, 125.0, 128.0, 122.0),
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

    val DESCENT = KustomFieldDefinition(
        typeId = "descent",
        childTitle = "Descent",
        category = FieldCategory.CLIMB,
        description = "Total elevation descended",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(310.0, 315.0, 320.0, 328.0, 322.0),
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

    val CLOCK_TIME = KustomFieldDefinition(
        typeId = "clock-time",
        childTitle = "Clock",
        category = FieldCategory.TIME,
        description = "Current time of day",
        supportsSmoothing = false,
        supportsZones = false,
        mockValues = listOf(34920.0, 34980.0, 35040.0, 35100.0, 35160.0),
    )

    val allFields: List<KustomFieldDefinition> = listOf(
        POWER, NORM_POWER, AVG_POWER, MAX_POWER, LAP_POWER, LAST_LAP_POWER, PERCENT_FTP, IF, TSS, POWER_TO_WEIGHT,
        HR, AVG_HR, MAX_HR, LAP_HR, PERCENT_MAX_HR,
        SPEED, AVG_SPEED, MAX_SPEED, LAP_SPEED, DISTANCE,
        CADENCE, AVG_CADENCE, MAX_CADENCE,
        GRADE, VAM, ELEVATION, ASCENT, DESCENT,
        RIDE_TIME, CLOCK_TIME,
    )

    val configurableFields: List<KustomFieldDefinition> = allFields.filter {
        it.supportsSmoothing || it.supportsZones
    }

    fun findByTypeId(typeId: String): KustomFieldDefinition? {
        return allFields.firstOrNull { it.typeId == typeId }
    }
}
