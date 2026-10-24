package com.hank.kustom.data.model

import io.hammerhead.karooext.models.DataType
import java.util.Locale

enum class FieldCategory(val displayName: String) {
    SPEED("Speed"),
    HEART_RATE("Heart Rate"),
    POWER("Power"),
    CADENCE("Cadence"),
    DISTANCE("Distance"),
    ELEVATION("Elevation"),
    TIME("Time"),
    BATTERY("Battery"),
    GEAR_RATIO("Gear Ratio"),
    ENVIRONMENT("Environment"),
    RADAR("Radar"),
    LAP("Lap"),
    WORKOUT("Workout"),
    NAVIGATION("Navigation"),
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
        get() = "${category.displayName.uppercase()}: ${childTitle.uppercase()}"

    fun resolveDynamicLabel(smoothing: SmoothingOption): String {
        return if (supportsSmoothing && smoothing != SmoothingOption.NONE) {
            "${smoothing.label} $childTitle"
        } else {
            childTitle
        }
    }

    fun formatNumericValueOnly(rawValue: Double, isImperial: Boolean = false): String {
        return when (typeId) {
            "speed", "avg-speed", "max-speed", "last-lap-speed" -> {
                val multiplier = if (isImperial) 2.23694 else 3.6
                val converted = rawValue * multiplier
                String.format(Locale.US, "%.1f", converted)
            }
            "distance", "lap-distance", "distance-remaining", "odometer", "nav-distance-to-next-turn" -> {
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
            "ride-time", "elapsed-time", "lap-time", "last-lap-time" -> {
                val totalSec = rawValue.toInt()
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                if (hrs > 0) String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
                else String.format(Locale.US, "%02d:%02d", mins, secs)
            }
            "clock-time", "nav-eta" -> {
                "09:42"
            }
            "intensity-factor", "gear-ratio" -> {
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
            "speed" -> smoothing.toSpeedDataTypeId()
            "avg-speed" -> DataType.Type.AVERAGE_SPEED
            "max-speed" -> DataType.Type.MAX_SPEED
            "hr" -> DataType.Type.HEART_RATE
            "avg-hr" -> DataType.Type.AVERAGE_HR
            "max-hr" -> DataType.Type.MAX_HR
            "hr-zone" -> DataType.Type.HR_ZONE
            "power" -> smoothing.toPowerDataTypeId()
            "avg-power" -> DataType.Type.AVERAGE_POWER
            "max-power" -> DataType.Type.MAX_POWER
            "np" -> DataType.Type.NORMALIZED_POWER
            "intensity-factor" -> DataType.Type.INTENSITY_FACTOR
            "tss" -> DataType.Type.TRAINING_STRESS_SCORE
            "power-balance" -> DataType.Type.PEDAL_POWER_BALANCE
            "power-zone" -> DataType.Type.POWER_ZONE
            "cadence" -> smoothing.toCadenceDataTypeId()
            "avg-cadence" -> DataType.Type.AVERAGE_CADENCE
            "max-cadence" -> DataType.Type.MAX_CADENCE
            "distance" -> DataType.Type.DISTANCE
            "lap-distance" -> DataType.Type.DISTANCE_LAP
            "elevation" -> DataType.Type.PRESSURE_ELEVATION_CORRECTION
            "ascent" -> DataType.Type.ELEVATION_GAIN
            "descent" -> DataType.Type.ELEVATION_LOSS
            "grade" -> DataType.Type.ELEVATION_GRADE
            "vam" -> DataType.Type.AVERAGE_VERTICAL_SPEED_30S
            "elapsed-time" -> DataType.Type.RIDE_TIME
            "ride-time" -> DataType.Type.ELAPSED_TIME
            "clock-time" -> DataType.Type.CLOCK_TIME
            "lap-time" -> DataType.Type.ELAPSED_TIME_LAP
            "battery-single" -> DataType.Type.BATTERY_PERCENT
            "battery-shifting" -> DataType.Type.SHIFTING_BATTERY
            "front-gear" -> DataType.Type.SHIFTING_FRONT_GEAR
            "rear-gear" -> DataType.Type.SHIFTING_REAR_GEAR
            "temperature" -> DataType.Type.TEMPERATURE
            "radar-threat-level" -> DataType.Type.RADAR
            "lap-number" -> DataType.Type.LAP_NUMBER
            "last-lap-time" -> DataType.Type.ELAPSED_TIME_LAST_LAP
            "last-lap-speed" -> DataType.Type.AVERAGE_SPEED_LAST_LAP
            "workout-primary-target" -> DataType.Type.WORKOUT_PRIMARY_TARGET
            "workout-next-target" -> DataType.Type.WORKOUT_SECONDARY_TARGET
            "workout-duration-remaining" -> DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION
            "nav-eta" -> DataType.Type.TIME_OF_ARRIVAL
            "nav-distance-to-next-turn" -> DataType.Type.DISTANCE_TO_NEXT_TURN
            "nav-heading" -> DataType.Type.HEADING
            else -> DataType.Type.POWER
        }
    }
}

object KustomFieldCatalog {

    // SPEED
    val SPEED = KustomFieldDefinition("speed", "Speed", FieldCategory.SPEED, "Current riding speed", supportsSmoothing = true, supportsZones = false, listOf(8.4, 8.8, 9.2, 9.5, 9.0))
    val AVG_SPEED = KustomFieldDefinition("avg-speed", "Avg", FieldCategory.SPEED, "Average ride speed", supportsSmoothing = true, supportsZones = false, listOf(8.1, 8.3, 8.5, 8.6, 8.4))
    val MAX_SPEED = KustomFieldDefinition("max-speed", "Max", FieldCategory.SPEED, "Maximum ride speed", supportsSmoothing = true, supportsZones = false, listOf(12.5, 13.8, 14.2, 15.1, 14.0))

    // HEART RATE
    val HR = KustomFieldDefinition("hr", "Heart Rate", FieldCategory.HEART_RATE, "Current heart rate", supportsSmoothing = false, supportsZones = true, listOf(145.0, 148.0, 152.0, 156.0, 150.0))
    val AVG_HR = KustomFieldDefinition("avg-hr", "Avg", FieldCategory.HEART_RATE, "Average ride heart rate", supportsSmoothing = false, supportsZones = true, listOf(140.0, 142.0, 145.0, 147.0, 144.0))
    val MAX_HR = KustomFieldDefinition("max-hr", "Max", FieldCategory.HEART_RATE, "Maximum ride heart rate", supportsSmoothing = false, supportsZones = true, listOf(172.0, 175.0, 178.0, 180.0, 177.0))
    val HR_ZONE = KustomFieldDefinition("hr-zone", "Zone", FieldCategory.HEART_RATE, "Current HR zone", supportsSmoothing = false, supportsZones = true, listOf(3.0, 3.2, 4.1, 4.5, 3.8))

    // POWER
    val POWER = KustomFieldDefinition("power", "Power", FieldCategory.POWER, "Current power output", supportsSmoothing = true, supportsZones = true, listOf(215.0, 228.0, 242.0, 260.0, 235.0))
    val AVG_POWER = KustomFieldDefinition("avg-power", "Avg", FieldCategory.POWER, "Average ride power", supportsSmoothing = true, supportsZones = true, listOf(210.0, 215.0, 218.0, 220.0, 216.0))
    val MAX_POWER = KustomFieldDefinition("max-power", "Max", FieldCategory.POWER, "Maximum ride power", supportsSmoothing = true, supportsZones = true, listOf(450.0, 520.0, 680.0, 710.0, 690.0))
    val NORM_POWER = KustomFieldDefinition("np", "NP", FieldCategory.POWER, "Normalized power", supportsSmoothing = true, supportsZones = true, listOf(220.0, 225.0, 230.0, 235.0, 228.0))
    val IF = KustomFieldDefinition("intensity-factor", "IF", FieldCategory.POWER, "Intensity factor", supportsSmoothing = true, supportsZones = true, listOf(0.82, 0.85, 0.88, 0.90, 0.86))
    val TSS = KustomFieldDefinition("tss", "TSS", FieldCategory.POWER, "Training stress score", supportsSmoothing = true, supportsZones = true, listOf(110.0, 115.0, 122.0, 130.0, 125.0))
    val POWER_BALANCE = KustomFieldDefinition("power-balance", "Balance", FieldCategory.POWER, "L/R power balance", supportsSmoothing = true, supportsZones = true, listOf(50.0, 51.0, 49.0, 52.0, 50.0))
    val POWER_ZONE = KustomFieldDefinition("power-zone", "Zone", FieldCategory.POWER, "Current power zone", supportsSmoothing = true, supportsZones = true, listOf(3.0, 3.5, 4.0, 4.2, 3.8))
    val KICKR_RESISTANCE = KustomFieldDefinition("kickr-resistance", "Kickr Resistance", FieldCategory.POWER, "Trainer resistance level", supportsSmoothing = false, supportsZones = false, listOf(25.0, 30.0, 35.0, 40.0, 35.0))

    // CADENCE
    val CADENCE = KustomFieldDefinition("cadence", "Cadence", FieldCategory.CADENCE, "Current pedaling cadence", supportsSmoothing = true, supportsZones = false, listOf(85.0, 88.0, 92.0, 95.0, 90.0))
    val AVG_CADENCE = KustomFieldDefinition("avg-cadence", "Avg", FieldCategory.CADENCE, "Average ride cadence", supportsSmoothing = true, supportsZones = false, listOf(84.0, 86.0, 88.0, 89.0, 87.0))
    val MAX_CADENCE = KustomFieldDefinition("max-cadence", "Max", FieldCategory.CADENCE, "Maximum ride cadence", supportsSmoothing = true, supportsZones = false, listOf(115.0, 120.0, 125.0, 128.0, 122.0))

    // DISTANCE
    val DISTANCE = KustomFieldDefinition("distance", "Distance", FieldCategory.DISTANCE, "Distance traveled", supportsSmoothing = false, supportsZones = false, listOf(12400.0, 12800.0, 13200.0, 13600.0, 14100.0))
    val DISTANCE_REMAINING = KustomFieldDefinition("distance-remaining", "Remaining", FieldCategory.DISTANCE, "Distance left on route", supportsSmoothing = false, supportsZones = false, listOf(8500.0, 8100.0, 7700.0, 7300.0, 6800.0))
    val LAP_DISTANCE = KustomFieldDefinition("lap-distance", "Lap Distance", FieldCategory.DISTANCE, "Distance traveled this lap", supportsSmoothing = false, supportsZones = false, listOf(3200.0, 3600.0, 4000.0, 4400.0, 4800.0))
    val ODOMETER = KustomFieldDefinition("odometer", "Odometer", FieldCategory.DISTANCE, "Total lifetime distance", supportsSmoothing = false, supportsZones = false, listOf(452000.0, 452005.0, 452010.0, 452015.0, 452020.0))

    // ELEVATION
    val ELEVATION = KustomFieldDefinition("elevation", "Elevation", FieldCategory.ELEVATION, "Current altitude", supportsSmoothing = true, supportsZones = false, listOf(320.0, 325.0, 332.0, 340.0, 345.0))
    val ASCENT = KustomFieldDefinition("ascent", "Ascent", FieldCategory.ELEVATION, "Total elevation gained", supportsSmoothing = true, supportsZones = false, listOf(450.0, 455.0, 462.0, 470.0, 475.0))
    val DESCENT = KustomFieldDefinition("descent", "Descent", FieldCategory.ELEVATION, "Total elevation descended", supportsSmoothing = true, supportsZones = false, listOf(310.0, 315.0, 320.0, 328.0, 322.0))
    val GRADE = KustomFieldDefinition("grade", "Grade", FieldCategory.ELEVATION, "Current road gradient", supportsSmoothing = true, supportsZones = false, listOf(4.2, 5.5, 6.8, 7.4, 5.1))
    val VAM = KustomFieldDefinition("vam", "VAM", FieldCategory.ELEVATION, "Vertical ascent speed", supportsSmoothing = true, supportsZones = false, listOf(850.0, 920.0, 1050.0, 1100.0, 980.0))

    // TIME
    val ELAPSED_TIME = KustomFieldDefinition("elapsed-time", "Elapsed Time", FieldCategory.TIME, "Total elapsed duration", supportsSmoothing = false, supportsZones = false, listOf(3600.0, 3605.0, 3610.0, 3615.0, 3620.0))
    val RIDE_TIME = KustomFieldDefinition("ride-time", "Riding Time", FieldCategory.TIME, "Active riding duration", supportsSmoothing = false, supportsZones = false, listOf(3400.0, 3405.0, 3410.0, 3415.0, 3420.0))
    val CLOCK_TIME = KustomFieldDefinition("clock-time", "Clock Time", FieldCategory.TIME, "Current time of day", supportsSmoothing = false, supportsZones = false, listOf(34920.0, 34980.0, 35040.0, 35100.0, 35160.0))
    val LAP_TIME = KustomFieldDefinition("lap-time", "Lap Time", FieldCategory.TIME, "Duration of current lap", supportsSmoothing = false, supportsZones = false, listOf(450.0, 455.0, 460.0, 465.0, 470.0))
    val TIME_AHEAD_BEHIND = KustomFieldDefinition("time-ahead-behind", "Time Ahead/Behind", FieldCategory.TIME, "Delta against target time", supportsSmoothing = false, supportsZones = false, listOf(12.0, 15.0, 18.0, 14.0, 10.0))

    // BATTERY
    val BATTERY_SINGLE = KustomFieldDefinition("battery-single", "Single", FieldCategory.BATTERY, "Karoo battery level", supportsSmoothing = false, supportsZones = false, listOf(88.0, 87.0, 86.0, 85.0, 84.0))
    val BATTERY_SHIFTING = KustomFieldDefinition("battery-shifting", "Shifting Battery", FieldCategory.BATTERY, "Drivetrain battery level", supportsSmoothing = false, supportsZones = false, listOf(100.0, 95.0, 90.0, 85.0, 80.0))
    val BATTERY_PHONE = KustomFieldDefinition("battery-phone", "Phone Battery", FieldCategory.BATTERY, "Connected phone battery", supportsSmoothing = false, supportsZones = false, listOf(72.0, 71.0, 70.0, 69.0, 68.0))

    // GEAR RATIO
    val FRONT_GEAR = KustomFieldDefinition("front-gear", "Front Gear", FieldCategory.GEAR_RATIO, "Current chainring teeth", supportsSmoothing = false, supportsZones = false, listOf(50.0, 50.0, 34.0, 34.0, 50.0))
    val REAR_GEAR = KustomFieldDefinition("rear-gear", "Rear Gear", FieldCategory.GEAR_RATIO, "Current cassette sprocket", supportsSmoothing = false, supportsZones = false, listOf(14.0, 16.0, 18.0, 21.0, 15.0))
    val GEAR_RATIO = KustomFieldDefinition("gear-ratio", "Gear Ratio", FieldCategory.GEAR_RATIO, "Front to rear gear ratio", supportsSmoothing = false, supportsZones = false, listOf(3.57, 3.12, 1.88, 1.62, 3.33))
    val GEAR_COMBINATION = KustomFieldDefinition("gear-combination", "Gear Combination", FieldCategory.GEAR_RATIO, "Current gear selection", supportsSmoothing = false, supportsZones = false, listOf(2.1, 2.2, 1.5, 1.4, 2.3))

    // ENVIRONMENT
    val TEMPERATURE = KustomFieldDefinition("temperature", "Temperature", FieldCategory.ENVIRONMENT, "Ambient air temperature", supportsSmoothing = false, supportsZones = false, listOf(22.0, 22.5, 23.0, 23.5, 22.8))
    val WIND_SPEED = KustomFieldDefinition("wind-speed", "Wind Speed", FieldCategory.ENVIRONMENT, "Current wind speed", supportsSmoothing = false, supportsZones = false, listOf(12.0, 14.0, 16.0, 18.0, 15.0))
    val WIND_DIRECTION = KustomFieldDefinition("wind-direction", "Wind Direction", FieldCategory.ENVIRONMENT, "Current wind bearing", supportsSmoothing = false, supportsZones = false, listOf(180.0, 190.0, 200.0, 210.0, 195.0))

    // RADAR
    val RADAR_VEHICLE_COUNT = KustomFieldDefinition("radar-vehicle-count", "Vehicle Count", FieldCategory.RADAR, "Approaching vehicles count", supportsSmoothing = false, supportsZones = false, listOf(0.0, 1.0, 2.0, 0.0, 1.0))
    val RADAR_THREAT_LEVEL = KustomFieldDefinition("radar-threat-level", "Threat Level", FieldCategory.RADAR, "Radar threat alert level", supportsSmoothing = false, supportsZones = false, listOf(0.0, 1.0, 2.0, 1.0, 0.0))
    val RADAR_VEHICLE_SPEED = KustomFieldDefinition("radar-vehicle-speed", "Vehicle Speed", FieldCategory.RADAR, "Fastest vehicle speed", supportsSmoothing = false, supportsZones = false, listOf(45.0, 52.0, 60.0, 48.0, 50.0))

    // LAP
    val LAP_NUMBER = KustomFieldDefinition("lap-number", "Lap Number", FieldCategory.LAP, "Current lap counter", supportsSmoothing = false, supportsZones = false, listOf(1.0, 2.0, 3.0, 4.0, 5.0))
    val LAST_LAP_TIME = KustomFieldDefinition("last-lap-time", "Last Lap Time", FieldCategory.LAP, "Duration of previous lap", supportsSmoothing = false, supportsZones = false, listOf(420.0, 425.0, 430.0, 435.0, 428.0))
    val LAST_LAP_SPEED = KustomFieldDefinition("last-lap-speed", "Last Lap Speed", FieldCategory.LAP, "Average speed of previous lap", supportsSmoothing = false, supportsZones = false, listOf(8.8, 9.1, 9.3, 9.5, 9.2))

    // WORKOUT
    val WORKOUT_PRIMARY_TARGET = KustomFieldDefinition("workout-primary-target", "Workout Primary Target", FieldCategory.WORKOUT, "Target power or heart rate", supportsSmoothing = false, supportsZones = true, listOf(250.0, 260.0, 270.0, 280.0, 265.0))
    val WORKOUT_NEXT_TARGET = KustomFieldDefinition("workout-next-target", "Workout Next Target", FieldCategory.WORKOUT, "Upcoming interval target", supportsSmoothing = false, supportsZones = false, listOf(180.0, 180.0, 280.0, 280.0, 180.0))
    val WORKOUT_DURATION_REMAINING = KustomFieldDefinition("workout-duration-remaining", "Workout Duration Remaining", FieldCategory.WORKOUT, "Remaining workout time", supportsSmoothing = false, supportsZones = false, listOf(1800.0, 1740.0, 1680.0, 1620.0, 1560.0))

    // NAVIGATION
    val NAV_ETA = KustomFieldDefinition("nav-eta", "ETA", FieldCategory.NAVIGATION, "Estimated arrival time", supportsSmoothing = false, supportsZones = false, listOf(36000.0, 36060.0, 36120.0, 36180.0, 36240.0))
    val NAV_DISTANCE_TO_NEXT_TURN = KustomFieldDefinition("nav-distance-to-next-turn", "Distance To Next Turn", FieldCategory.NAVIGATION, "Distance until next maneuver", supportsSmoothing = false, supportsZones = false, listOf(450.0, 400.0, 350.0, 300.0, 250.0))
    val NAV_HEADING = KustomFieldDefinition("nav-heading", "Heading", FieldCategory.NAVIGATION, "Compass heading direction", supportsSmoothing = false, supportsZones = false, listOf(90.0, 95.0, 100.0, 105.0, 98.0))

    val allFields: List<KustomFieldDefinition> = listOf(
        SPEED, AVG_SPEED, MAX_SPEED,
        HR, AVG_HR, MAX_HR, HR_ZONE,
        POWER, AVG_POWER, MAX_POWER, NORM_POWER, IF, TSS, POWER_BALANCE, POWER_ZONE, KICKR_RESISTANCE,
        CADENCE, AVG_CADENCE, MAX_CADENCE,
        DISTANCE, DISTANCE_REMAINING, LAP_DISTANCE, ODOMETER,
        ELEVATION, ASCENT, DESCENT, GRADE, VAM,
        ELAPSED_TIME, RIDE_TIME, CLOCK_TIME, LAP_TIME, TIME_AHEAD_BEHIND,
        BATTERY_SINGLE, BATTERY_SHIFTING, BATTERY_PHONE,
        FRONT_GEAR, REAR_GEAR, GEAR_RATIO, GEAR_COMBINATION,
        TEMPERATURE, WIND_SPEED, WIND_DIRECTION,
        RADAR_VEHICLE_COUNT, RADAR_THREAT_LEVEL, RADAR_VEHICLE_SPEED,
        LAP_NUMBER, LAST_LAP_TIME, LAST_LAP_SPEED,
        WORKOUT_PRIMARY_TARGET, WORKOUT_NEXT_TARGET, WORKOUT_DURATION_REMAINING,
        NAV_ETA, NAV_DISTANCE_TO_NEXT_TURN, NAV_HEADING,
    )

    val configurableFields: List<KustomFieldDefinition> = allFields.filter {
        it.supportsSmoothing || it.supportsZones
    }

    fun findByTypeId(typeId: String): KustomFieldDefinition? {
        return allFields.firstOrNull { it.typeId == typeId }
    }
}
