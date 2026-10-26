package com.hank.kustom.data.model

import io.hammerhead.karooext.models.DataType
import java.util.Locale

enum class FieldCategory(val displayName: String) {
    CADENCE("Cadence"),
    CLIMB("Climb"),
    GENERAL("General"),
    HEART_RATE("Heart Rate"),
    LAP("Lap"),
    NAVIGATION("Navigation"),
    POWER("Power"),
    SPEED("Speed"),
    TIME("Time"),
    WORKOUT("Workout"),
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
        get() = "${category.displayName} - $childTitle"

    val cardHeaderTitle: String
        get() = when (typeId) {
            "power" -> "Power"
            "np" -> "Normalized Power (NP)"
            "avg-power" -> "Average Power"
            "max-power" -> "Maximum Power"
            "power-20m" -> "20m Rolling Avg Power"
            "power-1hr" -> "1hr Rolling Avg Power"
            "power-balance" -> "Power Balance (L/R)"
            "percent-ftp" -> "Percent of FTP (% FTP)"
            "power-to-weight" -> "Power to Weight (W/kg)"
            "power-zone" -> "Power Zone"
            "intensity-factor" -> "Intensity Factor (IF)"
            "tss" -> "Training Stress Score (TSS)"
            "power-vi" -> "Variability Index (VI)"
            "power-kj" -> "Kilojoules (kJ)"
            "power-cal" -> "Calories"
            "power-cal-hr" -> "Calories / Hour"
            "power-smoothness" -> "Pedal Smoothness"
            "power-te" -> "Torque Effectiveness"
            "avg-power-balance" -> "Average Power Balance"

            "hr" -> "Heart Rate"
            "avg-hr" -> "Average Heart Rate"
            "max-hr" -> "Maximum Heart Rate"
            "hr-zone" -> "Heart Rate Zone"
            "percent-max-hr" -> "Percent of Max Heart Rate"
            "percent-hrr" -> "Percent of HR Reserve (% HRR)"
            "avg-percent-hrr" -> "Average % HR Reserve"

            "speed" -> "Current Speed"
            "avg-speed" -> "Average Speed"
            "max-speed" -> "Maximum Speed"
            "distance" -> "Distance Traveled"

            "cadence" -> "Cadence (RPM)"
            "torque" -> "Crank Torque"
            "avg-cadence" -> "Average Cadence"
            "max-cadence" -> "Maximum Cadence"
            "avg-torque" -> "Average Torque"
            "max-torque" -> "Maximum Torque"

            "grade" -> "Road Grade / Slope (%)"
            "vam" -> "Vertical Ascent Speed (VAM)"
            "elevation" -> "Current Elevation"
            "ascent" -> "Total Elevation Gain"
            "descent" -> "Total Elevation Loss"
            "avg-vam" -> "Average VAM"
            "dist-to-top" -> "Distance to Top of Climb"
            "elev-to-top" -> "Elevation Remaining on Climb"
            "climb-number" -> "Climb Counter Number"

            "lap-power" -> "Lap Average Power"
            "lap-np" -> "Lap Normalized Power"
            "lap-hr" -> "Lap Average Heart Rate"
            "last-lap-power" -> "Last Lap Average Power"
            "last-lap-np" -> "Last Lap Normalized Power"
            "last-lap-hr" -> "Last Lap Average Heart Rate"
            "lap-number" -> "Current Lap Counter"
            "lap-time" -> "Lap Duration"
            "lap-dist" -> "Lap Distance"
            "lap-speed" -> "Lap Average Speed"
            "lap-cadence" -> "Lap Average Cadence"
            "lap-ascent" -> "Lap Elevation Gain"
            "last-lap-time" -> "Last Lap Duration"
            "last-lap-dist" -> "Last Lap Distance"
            "last-lap-speed" -> "Last Lap Average Speed"

            "ride-time" -> "Active Riding Time"
            "elapsed-time" -> "Total Elapsed Time"
            "paused-time" -> "Total Paused Time"
            "clock-time" -> "Clock Time of Day"
            "sunrise" -> "Local Sunrise Time"
            "sunset" -> "Local Sunset Time"

            "nav-dist-dest" -> "Distance to Destination"
            "nav-dist-turn" -> "Distance to Next Turn"
            "nav-elev-rem" -> "Elevation Remaining on Route"
            "nav-time-dest" -> "Estimated Time to Destination"
            "nav-eta" -> "Estimated Time of Arrival (ETA)"
            "nav-heading" -> "Compass Heading"

            "shift-gears" -> "Front & Rear Gears"
            "shift-front" -> "Front Chainring Teeth"
            "shift-rear" -> "Rear Sprocket Teeth"
            "shift-batt" -> "Drivetrain Battery Level"
            "battery-pct" -> "Karoo Device Battery"
            "temperature" -> "Ambient Air Temperature"
            "psi-front" -> "Front Tire Pressure (PSI)"
            "psi-rear" -> "Rear Tire Pressure (PSI)"
            "core-temp" -> "Core Body Temperature"
            "ebike-batt" -> "eBike Battery Level"
            "ebike-range" -> "Estimated eBike Range"

            "workout-target-power" -> "Workout Target Power"
            "workout-target-hr" -> "Workout Target Heart Rate"
            "workout-target-cadence" -> "Workout Target Cadence"
            "workout-primary-target" -> "Workout Primary Target"
            "workout-interval-time-remaining" -> "Interval Time Remaining"
            "workout-duration-remaining" -> "Workout Duration Remaining"
            "workout-interval-count" -> "Workout Interval Count"
            "workout-next-target" -> "Workout Upcoming Target"

            else -> childTitle
        }

    fun resolveDynamicLabel(smoothing: SmoothingOption): String {
        return if (supportsSmoothing && smoothing != SmoothingOption.NONE) {
            "${smoothing.label} $childTitle"
        } else {
            childTitle
        }
    }

    fun formatNumericValueOnly(rawValue: Double, isImperial: Boolean = false): String {
        return when (typeId) {
            "speed", "avg-speed", "max-speed", "lap-speed", "last-lap-speed" -> {
                val multiplier = if (isImperial) 2.23694 else 3.6
                val converted = rawValue * multiplier
                String.format(Locale.US, "%.1f", converted)
            }
            "distance", "lap-dist", "last-lap-dist", "nav-dist-dest", "nav-dist-turn", "dist-to-top" -> {
                val multiplier = if (isImperial) 0.000621371 else 0.001
                val converted = rawValue * multiplier
                String.format(Locale.US, "%.1f", converted)
            }
            "grade" -> {
                String.format(Locale.US, "%.1f", rawValue)
            }
            "elevation", "ascent", "descent", "lap-ascent", "elev-to-top", "nav-elev-rem" -> {
                val multiplier = if (isImperial) 3.28084 else 1.0
                val converted = rawValue * multiplier
                converted.toInt().toString()
            }
            "temperature", "core-temp" -> {
                val converted = if (isImperial) (rawValue * 9.0 / 5.0 + 32.0) else rawValue
                String.format(Locale.US, "%.1f", converted)
            }
            "psi-front", "psi-rear" -> {
                val converted = if (isImperial) rawValue else (rawValue * 0.0689476)
                String.format(Locale.US, "%.1f", converted)
            }
            "ride-time", "elapsed-time", "paused-time", "lap-time", "last-lap-time", "workout-interval-time-remaining", "workout-duration-remaining" -> {
                val totalSec = rawValue.toInt()
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                if (hrs > 0) String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
                else String.format(Locale.US, "%02d:%02d", mins, secs)
            }
            "clock-time", "sunrise", "sunset", "nav-eta" -> {
                "09:42"
            }
            "intensity-factor", "power-vi" -> {
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
            "power-20m" -> DataType.Type.SMOOTHED_20M_AVERAGE_POWER
            "power-1hr" -> DataType.Type.SMOOTHED_1HR_AVERAGE_POWER
            "power-balance" -> smoothing.toCadenceDataTypeId()
            "percent-ftp" -> DataType.Type.PERCENT_MAX_FTP
            "power-to-weight" -> DataType.Type.POWER_TO_WEIGHT
            "power-zone" -> DataType.Type.POWER_ZONE
            "intensity-factor" -> DataType.Type.INTENSITY_FACTOR
            "tss" -> DataType.Type.TRAINING_STRESS_SCORE
            "power-vi" -> DataType.Type.VARIABILITY_INDEX
            "power-kj" -> DataType.Type.ENERGY_OUTPUT
            "power-cal" -> DataType.Type.CALORIES
            "power-cal-hr" -> DataType.Type.CALORIES_PER_HOUR
            "power-smoothness" -> DataType.Type.PEDAL_POWER_BALANCE
            "power-te" -> DataType.Type.POWER
            "avg-power-balance" -> DataType.Type.AVERAGE_PEDAL_POWER_BALANCE

            "hr" -> DataType.Type.HEART_RATE
            "avg-hr" -> DataType.Type.AVERAGE_HR
            "max-hr" -> DataType.Type.MAX_HR
            "hr-zone" -> DataType.Type.HR_ZONE
            "percent-max-hr" -> DataType.Type.PERCENT_MAX_HR
            "percent-hrr" -> DataType.Type.PERCENT_HRR
            "avg-percent-hrr" -> DataType.Type.AVERAGE_PERCENT_HRR

            "speed" -> smoothing.toSpeedDataTypeId()
            "avg-speed" -> DataType.Type.AVERAGE_SPEED
            "max-speed" -> DataType.Type.MAX_SPEED
            "distance" -> DataType.Type.DISTANCE

            "cadence" -> smoothing.toCadenceDataTypeId()
            "torque" -> DataType.Type.TORQUE
            "avg-cadence" -> DataType.Type.AVERAGE_CADENCE
            "max-cadence" -> DataType.Type.MAX_CADENCE
            "avg-torque" -> DataType.Type.TORQUE
            "max-torque" -> DataType.Type.TORQUE

            "grade" -> DataType.Type.ELEVATION_GRADE
            "vam" -> DataType.Type.AVERAGE_VERTICAL_SPEED_30S
            "elevation" -> DataType.Type.PRESSURE_ELEVATION_CORRECTION
            "ascent" -> DataType.Type.ELEVATION_GAIN
            "descent" -> DataType.Type.ELEVATION_LOSS
            "avg-vam" -> DataType.Type.AVERAGE_VERTICAL_SPEED
            "dist-to-top" -> DataType.Type.DISTANCE_TO_TOP
            "elev-to-top" -> DataType.Type.ELEVATION_TO_TOP
            "climb-number" -> DataType.Type.CLIMB_NUMBER

            "lap-power" -> DataType.Type.POWER_LAP
            "lap-np" -> DataType.Type.NORMALIZED_POWER_LAP
            "lap-hr" -> DataType.Type.AVERAGE_LAP_HR
            "last-lap-power" -> DataType.Type.AVERAGE_POWER_LAST_LAP
            "last-lap-np" -> DataType.Type.NORMALIZED_POWER_LAST_LAP
            "last-lap-hr" -> DataType.Type.AVERAGE_HR_LAST_LAP
            "lap-number" -> DataType.Type.LAP_NUMBER
            "lap-time" -> DataType.Type.ELAPSED_TIME_LAP
            "lap-dist" -> DataType.Type.DISTANCE_LAP
            "lap-speed" -> DataType.Type.AVERAGE_SPEED_LAP
            "lap-cadence" -> DataType.Type.CADENCE_LAP
            "lap-ascent" -> DataType.Type.ELEVATION_GAIN_LAP
            "last-lap-time" -> DataType.Type.ELAPSED_TIME_LAST_LAP
            "last-lap-dist" -> DataType.Type.DISTANCE_LAP_LAST_LAP
            "last-lap-speed" -> DataType.Type.AVERAGE_SPEED_LAST_LAP

            "ride-time" -> DataType.Type.ELAPSED_TIME
            "elapsed-time" -> DataType.Type.RIDE_TIME
            "paused-time" -> DataType.Type.PAUSED_TIME
            "clock-time" -> DataType.Type.CLOCK_TIME
            "sunrise" -> DataType.Type.SUNRISE
            "sunset" -> DataType.Type.SUNSET

            "nav-dist-dest" -> DataType.Type.DISTANCE_TO_DESTINATION
            "nav-dist-turn" -> DataType.Type.DISTANCE_TO_NEXT_TURN
            "nav-elev-rem" -> DataType.Type.ELEVATION_REMAINING
            "nav-time-dest" -> DataType.Type.TIME_TO_DESTINATION
            "nav-eta" -> DataType.Type.TIME_OF_ARRIVAL
            "nav-heading" -> DataType.Type.HEADING

            "shift-gears" -> DataType.Type.SHIFTING_GEARS
            "shift-front" -> DataType.Type.SHIFTING_FRONT_GEAR
            "shift-rear" -> DataType.Type.SHIFTING_REAR_GEAR
            "shift-batt" -> DataType.Type.SHIFTING_BATTERY
            "workout-target-power" -> smoothing.toPowerDataTypeId()
            "workout-target-hr" -> DataType.Type.HEART_RATE
            "workout-target-cadence" -> smoothing.toCadenceDataTypeId()
            "workout-primary-target" -> DataType.Type.WORKOUT_PRIMARY_TARGET
            "workout-interval-time-remaining" -> DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION
            "workout-duration-remaining" -> DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION
            "workout-interval-count" -> DataType.Type.WORKOUT_INTERVAL_COUNT
            "workout-next-target" -> DataType.Type.WORKOUT_SECONDARY_TARGET
            "battery-pct" -> DataType.Type.BATTERY_PERCENT
            "temperature" -> DataType.Type.TEMPERATURE
            "psi-front" -> DataType.Type.TIRE_PRESSURE_FRONT
            "psi-rear" -> DataType.Type.TIRE_PRESSURE_REAR
            "core-temp" -> DataType.Type.CORE_TEMP
            "ebike-batt" -> DataType.Type.LEV_BATTERY_STATUS
            "ebike-range" -> DataType.Type.LEV_ESTIMATED_RANGE

            else -> DataType.Type.POWER
        }
    }
}

object KustomFieldCatalog {

    // 1. CADENCE (FieldCategory.CADENCE)
    val AVG_CADENCE = KustomFieldDefinition("avg-cadence", "Avg Cadence", FieldCategory.CADENCE, "Average pedaling cadence", supportsSmoothing = false, supportsZones = false, listOf(84.0, 86.0, 88.0, 89.0, 87.0))
    val AVG_TORQUE = KustomFieldDefinition("avg-torque", "Avg Torque", FieldCategory.CADENCE, "Average crank torque", supportsSmoothing = false, supportsZones = false, listOf(18.0, 20.0, 22.0, 24.0, 21.0))
    val CADENCE = KustomFieldDefinition("cadence", "Cadence", FieldCategory.CADENCE, "Current pedaling cadence", supportsSmoothing = true, supportsZones = false, listOf(85.0, 88.0, 92.0, 95.0, 90.0))
    val MAX_CADENCE = KustomFieldDefinition("max-cadence", "Max Cadence", FieldCategory.CADENCE, "Maximum pedaling cadence", supportsSmoothing = false, supportsZones = false, listOf(115.0, 120.0, 125.0, 128.0, 122.0))
    val MAX_TORQUE = KustomFieldDefinition("max-torque", "Max Torque", FieldCategory.CADENCE, "Maximum crank torque", supportsSmoothing = false, supportsZones = false, listOf(45.0, 50.0, 55.0, 60.0, 52.0))
    val TORQUE = KustomFieldDefinition("torque", "Torque", FieldCategory.CADENCE, "Current crank torque", supportsSmoothing = true, supportsZones = false, listOf(18.0, 22.0, 25.0, 28.0, 24.0))

    // 2. CLIMB (FieldCategory.CLIMB)
    val ASCENT = KustomFieldDefinition("ascent", "Ascent", FieldCategory.CLIMB, "Total elevation gained", supportsSmoothing = false, supportsZones = false, listOf(450.0, 455.0, 462.0, 470.0, 475.0))
    val AVG_VAM = KustomFieldDefinition("avg-vam", "Avg VAM", FieldCategory.CLIMB, "Average vertical ascent speed", supportsSmoothing = false, supportsZones = false, listOf(800.0, 840.0, 880.0, 900.0, 860.0))
    val CLIMB_NUMBER = KustomFieldDefinition("climb-number", "Climb #", FieldCategory.CLIMB, "Current climb counter", supportsSmoothing = false, supportsZones = false, listOf(1.0, 1.0, 2.0, 2.0, 3.0))
    val DESCENT = KustomFieldDefinition("descent", "Descent", FieldCategory.CLIMB, "Total elevation descended", supportsSmoothing = false, supportsZones = false, listOf(310.0, 315.0, 320.0, 328.0, 322.0))
    val DIST_TO_TOP = KustomFieldDefinition("dist-to-top", "Dist To Top", FieldCategory.CLIMB, "Distance to top of climb", supportsSmoothing = false, supportsZones = false, listOf(1200.0, 1000.0, 800.0, 600.0, 400.0))
    val ELEV_TO_TOP = KustomFieldDefinition("elev-to-top", "Elev To Top", FieldCategory.CLIMB, "Elevation remaining on climb", supportsSmoothing = false, supportsZones = false, listOf(120.0, 100.0, 80.0, 60.0, 40.0))
    val ELEVATION = KustomFieldDefinition("elevation", "Elev", FieldCategory.CLIMB, "Current altitude", supportsSmoothing = false, supportsZones = false, listOf(320.0, 325.0, 332.0, 340.0, 345.0))
    val GRADE = KustomFieldDefinition("grade", "Grade", FieldCategory.CLIMB, "Current road gradient", supportsSmoothing = false, supportsZones = true, listOf(4.2, 5.5, 6.8, 7.4, 5.1))
    val VAM = KustomFieldDefinition("vam", "VAM", FieldCategory.CLIMB, "30s vertical ascent speed", supportsSmoothing = true, supportsZones = false, listOf(850.0, 920.0, 1050.0, 1100.0, 980.0))

    // 3. GENERAL (FieldCategory.GENERAL)
    val CORE_TEMP = KustomFieldDefinition("core-temp", "Core Temp", FieldCategory.GENERAL, "Core body temperature", supportsSmoothing = false, supportsZones = false, listOf(37.2, 37.4, 37.6, 37.8, 37.5))
    val SHIFT_BATT = KustomFieldDefinition("shift-batt", "Shift Batt", FieldCategory.GENERAL, "Shifting battery level %", supportsSmoothing = false, supportsZones = false, listOf(100.0, 95.0, 90.0, 85.0, 80.0))
    val EBIKE_BATT = KustomFieldDefinition("ebike-batt", "eBike Batt", FieldCategory.GENERAL, "eBike battery level %", supportsSmoothing = false, supportsZones = false, listOf(92.0, 90.0, 88.0, 85.0, 82.0))
    val EBIKE_RANGE = KustomFieldDefinition("ebike-range", "eBike Range", FieldCategory.GENERAL, "Estimated eBike range", supportsSmoothing = false, supportsZones = false, listOf(45.0, 43.0, 41.0, 39.0, 37.0))
    val SHIFT_FRONT = KustomFieldDefinition("shift-front", "Front Gear", FieldCategory.GENERAL, "Front chainring teeth", supportsSmoothing = false, supportsZones = false, listOf(50.0, 50.0, 34.0, 34.0, 50.0))
    val PSI_FRONT = KustomFieldDefinition("psi-front", "Front PSI", FieldCategory.GENERAL, "Front tire pressure", supportsSmoothing = false, supportsZones = false, listOf(82.0, 81.5, 81.0, 80.5, 81.2))
    val SHIFT_GEARS = KustomFieldDefinition("shift-gears", "Gears", FieldCategory.GENERAL, "Current front & rear gear", supportsSmoothing = false, supportsZones = false, listOf(5014.0, 5016.0, 3418.0, 3421.0, 5015.0))
    val BATTERY_PCT = KustomFieldDefinition("battery-pct", "Karoo Batt", FieldCategory.GENERAL, "Karoo battery level %", supportsSmoothing = false, supportsZones = false, listOf(88.0, 87.0, 86.0, 85.0, 84.0))
    val SHIFT_REAR = KustomFieldDefinition("shift-rear", "Rear Gear", FieldCategory.GENERAL, "Rear sprocket teeth", supportsSmoothing = false, supportsZones = false, listOf(14.0, 16.0, 18.0, 21.0, 15.0))
    val PSI_REAR = KustomFieldDefinition("psi-rear", "Rear PSI", FieldCategory.GENERAL, "Rear tire pressure", supportsSmoothing = false, supportsZones = false, listOf(85.0, 84.5, 84.0, 83.5, 84.2))
    val TEMPERATURE = KustomFieldDefinition("temperature", "Temp", FieldCategory.GENERAL, "Ambient air temperature", supportsSmoothing = false, supportsZones = false, listOf(22.0, 22.5, 23.0, 23.5, 22.8))

    // 4. HEART RATE (FieldCategory.HEART_RATE)
    val PERCENT_HRR = KustomFieldDefinition("percent-hrr", "% HRR", FieldCategory.HEART_RATE, "% of heart rate reserve", supportsSmoothing = false, supportsZones = false, listOf(70.0, 72.0, 75.0, 78.0, 74.0))
    val PERCENT_MAX_HR = KustomFieldDefinition("percent-max-hr", "% Max HR", FieldCategory.HEART_RATE, "% of maximum heart rate", supportsSmoothing = false, supportsZones = false, listOf(78.0, 80.0, 82.0, 85.0, 81.0))
    val AVG_PERCENT_HRR = KustomFieldDefinition("avg-percent-hrr", "Avg % HRR", FieldCategory.HEART_RATE, "Average % of HR reserve", supportsSmoothing = false, supportsZones = false, listOf(68.0, 70.0, 72.0, 74.0, 71.0))
    val AVG_HR = KustomFieldDefinition("avg-hr", "Avg HR", FieldCategory.HEART_RATE, "Average heart rate", supportsSmoothing = false, supportsZones = true, listOf(140.0, 142.0, 145.0, 147.0, 144.0))
    val HR = KustomFieldDefinition("hr", "HR", FieldCategory.HEART_RATE, "Current heart rate", supportsSmoothing = true, supportsZones = true, listOf(145.0, 148.0, 152.0, 156.0, 150.0))
    val HR_ZONE = KustomFieldDefinition("hr-zone", "HR Zone", FieldCategory.HEART_RATE, "Current HR zone", supportsSmoothing = false, supportsZones = false, listOf(3.0, 3.2, 4.1, 4.5, 3.8))
    val MAX_HR = KustomFieldDefinition("max-hr", "Max HR", FieldCategory.HEART_RATE, "Maximum heart rate", supportsSmoothing = false, supportsZones = true, listOf(172.0, 175.0, 178.0, 180.0, 177.0))

    // 5. LAP (FieldCategory.LAP)
    val LAP_ASCENT = KustomFieldDefinition("lap-ascent", "Lap Ascent", FieldCategory.LAP, "Elevation gained this lap", supportsSmoothing = false, supportsZones = false, listOf(80.0, 85.0, 90.0, 95.0, 88.0))
    val LAP_CADENCE = KustomFieldDefinition("lap-cadence", "Lap Cadence", FieldCategory.LAP, "Average cadence this lap", supportsSmoothing = false, supportsZones = false, listOf(86.0, 88.0, 90.0, 91.0, 89.0))
    val LAP_DIST = KustomFieldDefinition("lap-dist", "Lap Dist", FieldCategory.LAP, "Distance traveled this lap", supportsSmoothing = false, supportsZones = false, listOf(3200.0, 3600.0, 4000.0, 4400.0, 4800.0))
    val LAP_HR = KustomFieldDefinition("lap-hr", "Lap HR", FieldCategory.LAP, "Average HR this lap", supportsSmoothing = false, supportsZones = true, listOf(146.0, 149.0, 151.0, 153.0, 150.0))
    val LAP_NP = KustomFieldDefinition("lap-np", "Lap NP", FieldCategory.LAP, "Normalized power this lap", supportsSmoothing = false, supportsZones = true, listOf(225.0, 230.0, 235.0, 240.0, 232.0))
    val LAP_NUMBER = KustomFieldDefinition("lap-number", "Lap #", FieldCategory.LAP, "Current lap counter", supportsSmoothing = false, supportsZones = false, listOf(1.0, 2.0, 3.0, 4.0, 5.0))
    val LAP_POWER = KustomFieldDefinition("lap-power", "Lap Power", FieldCategory.LAP, "Average power this lap", supportsSmoothing = false, supportsZones = true, listOf(220.0, 225.0, 228.0, 232.0, 226.0))
    val LAP_SPEED = KustomFieldDefinition("lap-speed", "Lap Speed", FieldCategory.LAP, "Average speed this lap", supportsSmoothing = false, supportsZones = false, listOf(8.5, 8.7, 8.9, 9.1, 8.8))
    val LAP_TIME = KustomFieldDefinition("lap-time", "Lap Time", FieldCategory.LAP, "Duration of current lap", supportsSmoothing = false, supportsZones = false, listOf(450.0, 455.0, 460.0, 465.0, 470.0))
    val LAST_LAP_DIST = KustomFieldDefinition("last-lap-dist", "Last Lap Dist", FieldCategory.LAP, "Distance of previous lap", supportsSmoothing = false, supportsZones = false, listOf(3100.0, 3500.0, 3900.0, 4300.0, 4700.0))
    val LAST_LAP_HR = KustomFieldDefinition("last-lap-hr", "Last Lap HR", FieldCategory.LAP, "Average HR last lap", supportsSmoothing = false, supportsZones = true, listOf(144.0, 146.0, 148.0, 150.0, 147.0))
    val LAST_LAP_NP = KustomFieldDefinition("last-lap-np", "Last Lap NP", FieldCategory.LAP, "Normalized power last lap", supportsSmoothing = false, supportsZones = true, listOf(220.0, 222.0, 226.0, 230.0, 224.0))
    val LAST_LAP_POWER = KustomFieldDefinition("last-lap-power", "Last Lap Power", FieldCategory.LAP, "Average power last lap", supportsSmoothing = false, supportsZones = true, listOf(215.0, 218.0, 222.0, 225.0, 220.0))
    val LAST_LAP_SPEED = KustomFieldDefinition("last-lap-speed", "Last Lap Speed", FieldCategory.LAP, "Average speed of previous lap", supportsSmoothing = false, supportsZones = false, listOf(8.8, 9.1, 9.3, 9.5, 9.2))
    val LAST_LAP_TIME = KustomFieldDefinition("last-lap-time", "Last Lap Time", FieldCategory.LAP, "Duration of previous lap", supportsSmoothing = false, supportsZones = false, listOf(420.0, 425.0, 430.0, 435.0, 428.0))

    // 6. NAVIGATION (FieldCategory.NAVIGATION)
    val NAV_DIST_DEST = KustomFieldDefinition("nav-dist-dest", "Dist To Dest", FieldCategory.NAVIGATION, "Distance to route destination", supportsSmoothing = false, supportsZones = false, listOf(15400.0, 15000.0, 14600.0, 14200.0, 13800.0))
    val NAV_DIST_TURN = KustomFieldDefinition("nav-dist-turn", "Dist To Turn", FieldCategory.NAVIGATION, "Distance to next turn", supportsSmoothing = false, supportsZones = false, listOf(450.0, 400.0, 350.0, 300.0, 250.0))
    val NAV_ELEV_REM = KustomFieldDefinition("nav-elev-rem", "Elev Rem", FieldCategory.NAVIGATION, "Ascent remaining on route", supportsSmoothing = false, supportsZones = false, listOf(350.0, 340.0, 330.0, 320.0, 310.0))
    val NAV_ETA = KustomFieldDefinition("nav-eta", "ETA", FieldCategory.NAVIGATION, "Estimated arrival time", supportsSmoothing = false, supportsZones = false, listOf(36000.0, 36060.0, 36120.0, 36180.0, 36240.0))
    val NAV_HEADING = KustomFieldDefinition("nav-heading", "Heading", FieldCategory.NAVIGATION, "Compass heading direction", supportsSmoothing = false, supportsZones = false, listOf(90.0, 95.0, 100.0, 105.0, 98.0))
    val NAV_TIME_DEST = KustomFieldDefinition("nav-time-dest", "Time To Dest", FieldCategory.NAVIGATION, "Estimated time to destination", supportsSmoothing = false, supportsZones = false, listOf(2400.0, 2340.0, 2280.0, 2220.0, 2160.0))

    // 7. POWER (FieldCategory.POWER)
    val PERCENT_FTP = KustomFieldDefinition("percent-ftp", "% FTP", FieldCategory.POWER, "Percentage of FTP", supportsSmoothing = false, supportsZones = false, listOf(82.0, 85.0, 88.0, 92.0, 87.0))
    val POWER_1HR = KustomFieldDefinition("power-1hr", "1hr Power", FieldCategory.POWER, "1-hour rolling avg power", supportsSmoothing = false, supportsZones = true, listOf(225.0, 230.0, 232.0, 235.0, 228.0))
    val POWER_20M = KustomFieldDefinition("power-20m", "20m Power", FieldCategory.POWER, "20-minute rolling avg power", supportsSmoothing = false, supportsZones = true, listOf(240.0, 245.0, 250.0, 255.0, 248.0))
    val AVG_POWER_BALANCE = KustomFieldDefinition("avg-power-balance", "Avg Balance", FieldCategory.POWER, "Average L/R power balance", supportsSmoothing = false, supportsZones = false, listOf(50.0, 51.0, 50.0, 50.0, 50.0))
    val AVG_POWER = KustomFieldDefinition("avg-power", "Avg Power", FieldCategory.POWER, "Average ride power", supportsSmoothing = false, supportsZones = true, listOf(210.0, 215.0, 218.0, 220.0, 216.0))
    val POWER_CAL_HR = KustomFieldDefinition("power-cal-hr", "Cal/hr", FieldCategory.POWER, "Calorie burn rate per hour", supportsSmoothing = false, supportsZones = false, listOf(650.0, 680.0, 720.0, 750.0, 700.0))
    val POWER_CAL = KustomFieldDefinition("power-cal", "Calories", FieldCategory.POWER, "Calories burned", supportsSmoothing = false, supportsZones = false, listOf(820.0, 880.0, 930.0, 980.0, 900.0))
    val INTENSITY_FACTOR = KustomFieldDefinition("intensity-factor", "IF", FieldCategory.POWER, "Intensity factor", supportsSmoothing = false, supportsZones = false, listOf(0.82, 0.85, 0.88, 0.90, 0.86))
    val POWER_KJ = KustomFieldDefinition("power-kj", "kJ", FieldCategory.POWER, "Energy output in kilojoules", supportsSmoothing = false, supportsZones = false, listOf(850.0, 900.0, 950.0, 1000.0, 920.0))
    val MAX_POWER = KustomFieldDefinition("max-power", "Max Power", FieldCategory.POWER, "Maximum ride power", supportsSmoothing = false, supportsZones = true, listOf(450.0, 520.0, 680.0, 710.0, 690.0))
    val NP = KustomFieldDefinition("np", "NP", FieldCategory.POWER, "Normalized power", supportsSmoothing = false, supportsZones = true, listOf(220.0, 225.0, 230.0, 235.0, 228.0))
    val POWER_SMOOTHNESS = KustomFieldDefinition("power-smoothness", "Pedal Smooth", FieldCategory.POWER, "Pedal smoothness %", supportsSmoothing = false, supportsZones = false, listOf(22.0, 24.0, 26.0, 28.0, 25.0))
    val POWER = KustomFieldDefinition("power", "Power", FieldCategory.POWER, "Current power output", supportsSmoothing = true, supportsZones = true, listOf(215.0, 228.0, 242.0, 260.0, 235.0))
    val POWER_BALANCE = KustomFieldDefinition("power-balance", "Balance", FieldCategory.POWER, "L/R power balance %", supportsSmoothing = true, supportsZones = false, listOf(50.0, 51.0, 49.0, 52.0, 50.0))
    val POWER_ZONE = KustomFieldDefinition("power-zone", "Power Zone", FieldCategory.POWER, "Current power zone", supportsSmoothing = false, supportsZones = false, listOf(3.0, 3.5, 4.0, 4.2, 3.8))
    val POWER_TE = KustomFieldDefinition("power-te", "Torque Eff", FieldCategory.POWER, "Torque effectiveness %", supportsSmoothing = false, supportsZones = false, listOf(78.0, 80.0, 82.0, 85.0, 81.0))
    val TSS = KustomFieldDefinition("tss", "TSS", FieldCategory.POWER, "Training stress score", supportsSmoothing = false, supportsZones = false, listOf(110.0, 115.0, 122.0, 130.0, 125.0))
    val POWER_VI = KustomFieldDefinition("power-vi", "VI", FieldCategory.POWER, "Variability Index", supportsSmoothing = false, supportsZones = false, listOf(1.05, 1.08, 1.10, 1.12, 1.06))
    val POWER_TO_WEIGHT = KustomFieldDefinition("power-to-weight", "W/kg", FieldCategory.POWER, "Power output in W/kg", supportsSmoothing = false, supportsZones = false, listOf(3.2, 3.4, 3.6, 3.8, 3.5))

    // 8. SPEED (FieldCategory.SPEED)
    val AVG_SPEED = KustomFieldDefinition("avg-speed", "Avg Speed", FieldCategory.SPEED, "Average riding speed", supportsSmoothing = false, supportsZones = false, listOf(8.1, 8.3, 8.5, 8.6, 8.4))
    val DISTANCE = KustomFieldDefinition("distance", "Dist", FieldCategory.SPEED, "Distance traveled", supportsSmoothing = false, supportsZones = false, listOf(12400.0, 12800.0, 13200.0, 13600.0, 14100.0))
    val MAX_SPEED = KustomFieldDefinition("max-speed", "Max Speed", FieldCategory.SPEED, "Maximum riding speed", supportsSmoothing = false, supportsZones = false, listOf(12.5, 13.8, 14.2, 15.1, 14.0))
    val SPEED = KustomFieldDefinition("speed", "Speed", FieldCategory.SPEED, "Current riding speed", supportsSmoothing = false, supportsZones = false, listOf(8.4, 8.8, 9.2, 9.5, 9.0))

    // 9. TIME (FieldCategory.TIME)
    val CLOCK_TIME = KustomFieldDefinition("clock-time", "Clock", FieldCategory.TIME, "Current time of day", supportsSmoothing = false, supportsZones = false, listOf(34920.0, 34980.0, 35040.0, 35100.0, 35160.0))
    val ELAPSED_TIME = KustomFieldDefinition("elapsed-time", "Elapsed", FieldCategory.TIME, "Total elapsed duration", supportsSmoothing = false, supportsZones = false, listOf(3600.0, 3605.0, 3610.0, 3615.0, 3620.0))
    val PAUSED_TIME = KustomFieldDefinition("paused-time", "Paused", FieldCategory.TIME, "Total paused duration", supportsSmoothing = false, supportsZones = false, listOf(200.0, 200.0, 200.0, 200.0, 200.0))
    val RIDE_TIME = KustomFieldDefinition("ride-time", "Ride Time", FieldCategory.TIME, "Active riding duration", supportsSmoothing = false, supportsZones = false, listOf(3400.0, 3405.0, 3410.0, 3415.0, 3420.0))
    val SUNRISE = KustomFieldDefinition("sunrise", "Sunrise", FieldCategory.TIME, "Local sunrise time", supportsSmoothing = false, supportsZones = false, listOf(23400.0, 23400.0, 23400.0, 23400.0, 23400.0))
    val SUNSET = KustomFieldDefinition("sunset", "Sunset", FieldCategory.TIME, "Local sunset time", supportsSmoothing = false, supportsZones = false, listOf(72000.0, 72000.0, 72000.0, 72000.0, 72000.0))

    // 10. WORKOUT (FieldCategory.WORKOUT)
    val WORKOUT_INTERVAL_COUNT = KustomFieldDefinition("workout-interval-count", "Int Count", FieldCategory.WORKOUT, "Current interval step count", supportsSmoothing = false, supportsZones = false, listOf(3.0, 3.0, 4.0, 4.0, 5.0))
    val WORKOUT_INTERVAL_TIME = KustomFieldDefinition("workout-interval-time-remaining", "Int Rem", FieldCategory.WORKOUT, "Time remaining in current interval", supportsSmoothing = false, supportsZones = false, listOf(185.0, 180.0, 175.0, 170.0, 165.0))
    val WORKOUT_NEXT_TARGET = KustomFieldDefinition("workout-next-target", "Next Target", FieldCategory.WORKOUT, "Upcoming interval target", supportsSmoothing = false, supportsZones = false, listOf(180.0, 180.0, 280.0, 280.0, 180.0))
    val WORKOUT_POWER = KustomFieldDefinition("workout-target-power", "Target Power", FieldCategory.WORKOUT, "Target power label with current power value", supportsSmoothing = true, supportsZones = true, listOf(215.0, 228.0, 242.0, 260.0, 235.0))
    val WORKOUT_PRIMARY_TARGET = KustomFieldDefinition("workout-primary-target", "Target", FieldCategory.WORKOUT, "Target power or heart rate", supportsSmoothing = false, supportsZones = true, listOf(250.0, 260.0, 270.0, 280.0, 265.0))
    val WORKOUT_TARGET_CADENCE = KustomFieldDefinition("workout-target-cadence", "Target Cadence", FieldCategory.WORKOUT, "Target cadence label with current cadence value", supportsSmoothing = true, supportsZones = false, listOf(85.0, 88.0, 92.0, 95.0, 90.0))
    val WORKOUT_TARGET_HR = KustomFieldDefinition("workout-target-hr", "Target HR", FieldCategory.WORKOUT, "Target HR label with current HR value", supportsSmoothing = false, supportsZones = true, listOf(145.0, 148.0, 152.0, 156.0, 150.0))
    val WORKOUT_DURATION_REMAINING = KustomFieldDefinition("workout-duration-remaining", "Wrk Rem", FieldCategory.WORKOUT, "Remaining workout time", supportsSmoothing = false, supportsZones = false, listOf(1800.0, 1740.0, 1680.0, 1620.0, 1560.0))

    /** Complete list of all 75 registered data fields sorted in alphabetical order by parent and child. */
    val allFields: List<KustomFieldDefinition> = listOf(
        // 1. CADENCE
        AVG_CADENCE, AVG_TORQUE, CADENCE, MAX_CADENCE, MAX_TORQUE, TORQUE,
        // 2. CLIMB
        ASCENT, AVG_VAM, CLIMB_NUMBER, DESCENT, DIST_TO_TOP, ELEV_TO_TOP, ELEVATION, GRADE, VAM,
        // 3. GENERAL
        CORE_TEMP, SHIFT_BATT, EBIKE_BATT, EBIKE_RANGE, SHIFT_FRONT, PSI_FRONT, SHIFT_GEARS, BATTERY_PCT, SHIFT_REAR, PSI_REAR, TEMPERATURE,
        // 4. HEART RATE
        PERCENT_HRR, PERCENT_MAX_HR, AVG_PERCENT_HRR, AVG_HR, HR, HR_ZONE, MAX_HR,
        // 5. LAP
        LAP_ASCENT, LAP_CADENCE, LAP_DIST, LAP_HR, LAP_NP, LAP_NUMBER, LAP_POWER, LAP_SPEED, LAP_TIME, LAST_LAP_DIST, LAST_LAP_HR, LAST_LAP_NP, LAST_LAP_POWER, LAST_LAP_SPEED, LAST_LAP_TIME,
        // 6. NAVIGATION
        NAV_DIST_DEST, NAV_DIST_TURN, NAV_ELEV_REM, NAV_ETA, NAV_HEADING, NAV_TIME_DEST,
        // 7. POWER
        PERCENT_FTP, POWER_1HR, POWER_20M, AVG_POWER_BALANCE, AVG_POWER, POWER_CAL_HR, POWER_CAL, INTENSITY_FACTOR, POWER_KJ, MAX_POWER, NP, POWER_SMOOTHNESS, POWER, POWER_BALANCE, POWER_ZONE, POWER_TE, TSS, POWER_VI, POWER_TO_WEIGHT,
        // 8. SPEED
        AVG_SPEED, DISTANCE, MAX_SPEED, SPEED,
        // 9. TIME
        CLOCK_TIME, ELAPSED_TIME, PAUSED_TIME, RIDE_TIME, SUNRISE, SUNSET,
        // 10. WORKOUT
        WORKOUT_INTERVAL_COUNT, WORKOUT_INTERVAL_TIME, WORKOUT_NEXT_TARGET, WORKOUT_POWER, WORKOUT_PRIMARY_TARGET, WORKOUT_TARGET_CADENCE, WORKOUT_TARGET_HR, WORKOUT_DURATION_REMAINING,
    )

    /** Configurable data fields with smoothing or zone capabilities. */
    val configurableFields: List<KustomFieldDefinition> = allFields.filter {
        it.supportsSmoothing || it.supportsZones
    }

    /**
     * Resolves a [KustomFieldDefinition] by its unique [typeId].
     *
     * @param typeId Field identifier string.
     * @return Matching [KustomFieldDefinition], or null if unmapped.
     */
    fun findByTypeId(typeId: String): KustomFieldDefinition? {
        return allFields.firstOrNull { it.typeId == typeId }
    }
}
