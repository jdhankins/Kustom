package com.hank.kustom.domain

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZonePalette
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile

/**
 * Resolved display state model for a single Kustom data field.
 */
data class ResolvedFieldDisplayState(
    val fieldId: String,
    val label: String,
    val rawValueText: String,
    val primaryValue: String,
    val secondarySuperscript: String?,
    val backgroundColor: Int,
    val valueTextColor: Int,
    val labelTextColor: Int,
    val dividerColor: Int,
    val zoneColor: Int?,
    val activeZoneIndex: Int?,
    val isColorBackgroundActive: Boolean,
    val formattedZoneText: String? = null,
)

data class QuadColors(
    val backgroundColor: Int,
    val valueTextColor: Int,
    val labelTextColor: Int,
    val dividerColor: Int,
    val isColorBackgroundActive: Boolean,
)

/**
 * Single deterministic source of truth for field display state resolution.
 */
object KustomFieldStateResolver {

    fun resolveValueString(
        definition: KustomFieldDefinition,
        streamState: StreamState?,
        dynamicValue: Double?,
        isImperial: Boolean,
        hasEverStreamed: Boolean = false,
        isInRide: Boolean = true,
        isMock: Boolean = false,
        isWorkoutActiveOnKaroo: Boolean = false,
    ): String {
        val isWorkoutField = definition.typeId == "workout-interval-time-remaining" ||
            definition.typeId == "workout-duration-remaining" ||
            definition.typeId.startsWith("workout-")

        val isSensorField = when (definition.typeId) {
            "power", "np", "avg-power", "max-power", "power-20m", "power-1hr", "power-balance",
            "percent-ftp", "power-to-weight", "power-zone", "intensity-factor", "tss", "power-vi",
            "power-kj", "power-cal", "power-cal-hr", "power-smoothness", "power-te", "avg-power-balance",
            "hr", "avg-hr", "max-hr", "hr-zone", "percent-max-hr", "percent-hrr", "avg-percent-hrr",
            "cadence", "torque", "avg-cadence", "max-cadence", "avg-torque", "max-torque",
            "psi-front", "psi-rear", "core-temp", "shift-gears", "shift-front", "shift-rear", "shift-batt",
            "ebike-batt", "ebike-range", "workout-target-power", "workout-target-hr", "workout-target-cadence" -> true
            else -> false
        }

        val isCoastingMetric = when (definition.typeId) {
            "cadence", "avg-cadence", "max-cadence", "power", "speed", "torque" -> true
            else -> false
        }

        return when {
            isMock -> {
                val v = dynamicValue ?: definition.mockValues.firstOrNull() ?: 228.0
                definition.formatNumericValueOnly(v, isImperial)
            }
            isWorkoutField && !isWorkoutActiveOnKaroo -> {
                "No Workout"
            }
            dynamicValue != null && !dynamicValue.isNaN() && (!isWorkoutField || dynamicValue >= 0.0) -> {
                definition.formatNumericValueOnly(dynamicValue, isImperial)
            }
            isCoastingMetric && (hasEverStreamed || isInRide) -> {
                definition.formatNumericValueOnly(0.0, isImperial)
            }
            streamState is StreamState.Idle -> {
                if (isWorkoutField) "No Workout" else definition.formatNumericValueOnly(0.0, isImperial)
            }
            streamState is StreamState.Streaming -> {
                if (isWorkoutField) {
                    "No Workout"
                } else if (isSensorField) {
                    "Searching..."
                } else {
                    definition.formatNumericValueOnly(0.0, isImperial)
                }
            }
            streamState is StreamState.Searching -> {
                if (isWorkoutField) "No Workout" else if (isSensorField) "Searching..." else definition.formatNumericValueOnly(0.0, isImperial)
            }
            streamState is StreamState.NotAvailable -> {
                if (isWorkoutField) "No Workout" else if (isSensorField) "No Sensor" else definition.formatNumericValueOnly(0.0, isImperial)
            }
            else -> {
                if (isWorkoutField) "No Workout" else if (isSensorField) "No Sensor" else "--"
            }
        }
    }

    fun resolveZoneInfo(
        fieldId: String,
        dynamicValue: Double?,
        userProfile: UserProfile?,
        zonePalette: ZonePalette,
        colorMode: ColorMode,
        isDarkTheme: Boolean = false,
    ): Triple<Int?, Int?, String?> {
        if (dynamicValue == null || dynamicValue.isNaN() || colorMode == ColorMode.NONE) {
            return Triple(null, null, null)
        }

        val zoneColor: Int?
        val activeIndex: Int?

        when {
            fieldId == "power" || fieldId == "np" || fieldId == "avg-power" || fieldId == "max-power" ||
                fieldId == "power-20m" || fieldId == "power-1hr" || fieldId == "power-combo" ||
                fieldId == "custom-combo" || fieldId == "percent-ftp" -> {
                zoneColor = KustomColorGradeMapper.getPowerZoneColor(
                    watts = dynamicValue,
                    userProfile = userProfile,
                    colorMode = colorMode,
                    isDarkTheme = isDarkTheme,
                    palette = zonePalette,
                )
                val ftp = (userProfile?.ftp ?: 250).toDouble().coerceAtLeast(50.0)
                val roundedWatts = kotlin.math.round(dynamicValue)
                val pctFtp = roundedWatts / ftp
                val pZones = userProfile?.powerZones
                activeIndex = if (!pZones.isNullOrEmpty()) {
                    val firstMin = pZones.first().min.toDouble()
                    val isRatio = pZones.first().max <= 5
                    if (isRatio) {
                        if (pctFtp <= firstMin) 0
                        else pZones.indexOfFirst { (_, max) -> pctFtp <= max.toDouble() }.let { if (it != -1) it else pZones.lastIndex }
                    } else {
                        if (roundedWatts <= firstMin) 0
                        else pZones.indexOfFirst { (_, max) -> roundedWatts <= max.toDouble() }.let { if (it != -1) it else pZones.lastIndex }
                    }
                } else {
                    when {
                        pctFtp < 0.55 -> 0
                        pctFtp < 0.75 -> 1
                        pctFtp < 0.90 -> 2
                        pctFtp < 1.05 -> 3
                        pctFtp < 1.20 -> 4
                        pctFtp < 1.50 -> 5
                        else -> 6
                    }
                }
            }
            fieldId == "hr" || fieldId == "avg-hr" || fieldId == "max-hr" ||
                fieldId == "hr-combo" || fieldId == "percent-max-hr" || fieldId == "percent-hrr" -> {
                zoneColor = KustomColorGradeMapper.getHeartRateZoneColor(
                    bpm = dynamicValue,
                    userProfile = userProfile,
                    colorMode = colorMode,
                    isDarkTheme = isDarkTheme,
                    palette = zonePalette,
                )
                val maxHr = (userProfile?.maxHr ?: 190).toDouble().coerceAtLeast(100.0)
                val roundedBpm = kotlin.math.round(dynamicValue)
                val pctMaxHr = roundedBpm / maxHr
                val hrZones = userProfile?.heartRateZones
                activeIndex = if (!hrZones.isNullOrEmpty()) {
                    val firstMin = hrZones.first().min.toDouble()
                    val isRatio = hrZones.first().max <= 5
                    if (isRatio) {
                        if (pctMaxHr <= firstMin) 0
                        else hrZones.indexOfFirst { (_, max) -> pctMaxHr <= max.toDouble() }.let { if (it != -1) it else hrZones.lastIndex }
                    } else {
                        if (roundedBpm <= firstMin) 0
                        else hrZones.indexOfFirst { (_, max) -> roundedBpm <= max.toDouble() }.let { if (it != -1) it else hrZones.lastIndex }
                    }
                } else {
                    when {
                        pctMaxHr < 0.60 -> 0
                        pctMaxHr < 0.70 -> 1
                        pctMaxHr < 0.80 -> 2
                        pctMaxHr < 0.90 -> 3
                        else -> 4
                    }
                }
            }
            fieldId == "grade" -> {
                zoneColor = KustomColorGradeMapper.getGradeColor(
                    gradePercent = dynamicValue,
                    colorMode = colorMode,
                    isDarkTheme = isDarkTheme,
                    palette = zonePalette,
                )
                activeIndex = null
            }
            else -> {
                zoneColor = null
                activeIndex = null
            }
        }

        val formattedText = if (activeIndex != null && activeIndex >= 0) "Z${activeIndex + 1}" else null
        return Triple(zoneColor, activeIndex, formattedText)
    }

    fun resolveColors(
        isDarkTheme: Boolean,
        colorMode: ColorMode,
        zoneColor: Int?,
        globalColors: GlobalColorConfig = GlobalColorConfig(),
    ): QuadColors {
        val defaultBgColor = if (isDarkTheme) globalColors.getDarkColorInt() else globalColors.getLightColorInt()
        val defaultTextColor = KustomColorGradeMapper.getContrastingTextColor(defaultBgColor)

        val isBgZoneFilled = colorMode == ColorMode.BACKGROUND_COLOR && zoneColor != null
        val backgroundColor = if (isBgZoneFilled) zoneColor!! else defaultBgColor

        val contrastTextColor = if (isBgZoneFilled) {
            val spec = globalColors.globalZonePalette.getNeonGlassSpecForColor(zoneColor!!)
            val checkColor = if (globalColors.globalZonePalette == ZonePalette.NEON_GLASS) spec.coreColor else zoneColor
            KustomColorGradeMapper.getContrastingTextColor(checkColor)
        } else {
            defaultTextColor
        }

        val labelTextColor = contrastTextColor
        val valueTextColor = when (colorMode) {
            ColorMode.NONE -> defaultTextColor
            ColorMode.TEXT_COLOR -> zoneColor ?: defaultTextColor
            ColorMode.BACKGROUND_COLOR -> contrastTextColor
        }

        val darkGreyBorder = if (isDarkTheme) 0xFF475569.toInt() else 0xFF64748B.toInt()
        val dividerColor = if (isBgZoneFilled) contrastTextColor else darkGreyBorder

        return QuadColors(
            backgroundColor = backgroundColor,
            valueTextColor = valueTextColor,
            labelTextColor = labelTextColor,
            dividerColor = dividerColor,
            isColorBackgroundActive = isBgZoneFilled,
        )
    }

    fun splitPrimaryAndSuperscript(rawValueText: String): Pair<String, String?> {
        if (rawValueText == "Searching..." || rawValueText == "No Sensor" || rawValueText == "No Workout" || rawValueText == "--") {
            return Pair(rawValueText, null)
        }
        val (pri, sec) = FullWidthLayoutEngine.parsePrimaryAndSecondary(rawValueText)
        val cleanedSec = sec.removePrefix(".").removePrefix(":").ifEmpty { null }
        return Pair(pri, cleanedSec)
    }

    fun resolveSingleFieldState(
        definition: KustomFieldDefinition,
        streamState: StreamState?,
        dynamicValue: Double?,
        isImperial: Boolean,
        colorMode: ColorMode,
        userProfile: UserProfile?,
        globalColors: GlobalColorConfig,
        smoothing: SmoothingOption = SmoothingOption.NONE,
        showHeader: Boolean = true,
        hasEverStreamed: Boolean = false,
        isInRide: Boolean = true,
        isDarkTheme: Boolean = false,
        isMock: Boolean = false,
        isWorkoutActiveOnKaroo: Boolean = false,
    ): ResolvedFieldDisplayState {
        val labelText = if (showHeader) definition.resolveDynamicLabel(smoothing) else ""
        val rawText = resolveValueString(
            definition = definition,
            streamState = streamState,
            dynamicValue = dynamicValue,
            isImperial = isImperial,
            hasEverStreamed = hasEverStreamed,
            isInRide = isInRide,
            isMock = isMock,
            isWorkoutActiveOnKaroo = isWorkoutActiveOnKaroo,
        )

        val (priVal, secSup) = splitPrimaryAndSuperscript(rawText)

        val (zoneColor, activeIndex, zoneText) = resolveZoneInfo(
            fieldId = definition.typeId,
            dynamicValue = dynamicValue,
            userProfile = userProfile,
            zonePalette = globalColors.globalZonePalette,
            colorMode = colorMode,
            isDarkTheme = isDarkTheme,
        )

        val colors = resolveColors(
            isDarkTheme = isDarkTheme,
            colorMode = colorMode,
            zoneColor = zoneColor,
            globalColors = globalColors,
        )

        return ResolvedFieldDisplayState(
            fieldId = definition.typeId,
            label = labelText,
            rawValueText = rawText,
            primaryValue = priVal,
            secondarySuperscript = secSup,
            backgroundColor = colors.backgroundColor,
            valueTextColor = colors.valueTextColor,
            labelTextColor = colors.labelTextColor,
            dividerColor = colors.dividerColor,
            zoneColor = zoneColor,
            activeZoneIndex = activeIndex,
            isColorBackgroundActive = colors.isColorBackgroundActive,
            formattedZoneText = zoneText,
        )
    }
}
