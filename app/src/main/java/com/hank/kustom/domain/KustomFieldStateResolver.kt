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
            streamState is StreamState.NotAvailable || streamState is StreamState.Idle -> {
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
        if (dynamicValue == null || colorMode == ColorMode.NONE) return Triple(null, null, null)

        val zoneColor: Int?
        val activeIndex: Int?

        when {
            fieldId == "power" || fieldId == "np" || fieldId == "avg-power" || fieldId == "max-power" ||
                fieldId == "power-20m" || fieldId == "power-1hr" || fieldId == "power-combo" || fieldId == "custom-combo" || fieldId == "percent-ftp" -> {
                zoneColor = KustomColorGradeMapper.getPowerZoneColor(dynamicValue, userProfile, colorMode, isDarkTheme, zonePalette)
                val ftp = (userProfile?.ftp ?: 250).toDouble().coerceAtLeast(50.0)
                val roundedWatts = kotlin.math.round(dynamicValue)
                val pctFtp = roundedWatts / ftp
                val pZones = userProfile?.powerZones
                activeIndex = if (!pZones.isNullOrEmpty()) {
                    val firstMin = pZones.first().min.toDouble()
                    val isRatio = pZones.first().max <= 5
                    if (isRatio) {
                        if (pctFtp <= firstMin) 0 else pZones.indexOfFirst { (_, max) -> pctFtp <= max.toDouble() }.let { if (it != -1) it else pZones.lastIndex }
                    } else {
                        if (roundedWatts <= firstMin) 0 else pZones.indexOfFirst { (_, max) -> roundedWatts <= max.toDouble() }.let { if (it != -1) it else pZones.lastIndex }
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
            fieldId == "hr" || fieldId == "avg-hr" || fieldId == "max-hr" || fieldId == "percent-max-hr" || fieldId == "percent-hrr" || fieldId == "avg-percent-hrr" -> {
                zoneColor = KustomColorGradeMapper.getHeartRateZoneColor(dynamicValue, userProfile, colorMode, isDarkTheme, zonePalette)
                val roundedBpm = kotlin.math.round(dynamicValue)
                val hrZones = userProfile?.heartRateZones
                activeIndex = if (!hrZones.isNullOrEmpty()) {
                    val firstMin = hrZones.first().min.toDouble()
                    if (roundedBpm <= firstMin) 0 else hrZones.indexOfFirst { (_, max) -> roundedBpm <= max.toDouble() }.let { if (it != -1) it else hrZones.lastIndex }
                } else {
                    val maxHr = (userProfile?.maxHr ?: 185).toDouble()
                    when {
                        roundedBpm < 0.60 * maxHr -> 0
                        roundedBpm < 0.70 * maxHr -> 1
                        roundedBpm < 0.80 * maxHr -> 2
                        roundedBpm < 0.90 * maxHr -> 3
                        roundedBpm < 0.95 * maxHr -> 4
                        else -> 5
                    }
                }
            }
            fieldId == "grade" -> {
                zoneColor = KustomColorGradeMapper.getGradeColor(dynamicValue, colorMode, isDarkTheme, zonePalette)
                val colors = zonePalette.get7ZoneColors()
                activeIndex = when {
                    dynamicValue < -2.0 -> 0
                    dynamicValue in -2.0..2.0 -> null
                    dynamicValue in 2.0..4.0 -> 2
                    dynamicValue in 4.0..7.0 -> 3
                    dynamicValue in 7.0..10.0 -> 4
                    dynamicValue in 10.0..14.0 -> 5
                    else -> 6
                }
            }
            else -> {
                zoneColor = null
                activeIndex = null
            }
        }

        val formattedText = if (activeIndex != null) "Z${activeIndex + 1}" else null
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
        val dividerColor = if (isBgZoneFilled) KustomColorGradeMapper.getContrastingTextColor(backgroundColor) else darkGreyBorder

        return QuadColors(
            backgroundColor = backgroundColor,
            valueTextColor = valueTextColor,
            labelTextColor = labelTextColor,
            dividerColor = dividerColor,
            isColorBackgroundActive = isBgZoneFilled,
        )
    }

    data class QuadColors(
        val backgroundColor: Int,
        val valueTextColor: Int,
        val labelTextColor: Int,
        val dividerColor: Int,
        val isColorBackgroundActive: Boolean,
    )

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
        isDarkTheme: Boolean = false,
        hasEverStreamed: Boolean = false,
        isInRide: Boolean = true,
        isMock: Boolean = false,
        isWorkoutActiveOnKaroo: Boolean = false,
    ): ResolvedFieldDisplayState {
        val labelText = if (showHeader) definition.resolveDynamicLabel(smoothing) else ""
        val rawValueText = resolveValueString(
            definition = definition,
            streamState = streamState,
            dynamicValue = dynamicValue,
            isImperial = isImperial,
            hasEverStreamed = hasEverStreamed,
            isInRide = isInRide,
            isMock = isMock,
            isWorkoutActiveOnKaroo = isWorkoutActiveOnKaroo,
        )

        val (primaryVal, secVal) = FullWidthLayoutEngine.parsePrimaryAndSecondary(rawValueText)

        val (zoneColor, activeIdx, formattedZoneText) = resolveZoneInfo(
            fieldId = definition.typeId,
            dynamicValue = dynamicValue,
            userProfile = userProfile,
            zonePalette = globalColors.globalZonePalette,
            colorMode = colorMode,
            isDarkTheme = isDarkTheme,
        )

        val (bg, valColor, lblColor, divColor, isBgActive) = resolveColors(
            isDarkTheme = isDarkTheme,
            colorMode = colorMode,
            zoneColor = zoneColor,
            globalColors = globalColors,
        )

        return ResolvedFieldDisplayState(
            fieldId = definition.typeId,
            label = labelText,
            rawValueText = rawValueText,
            primaryValue = primaryVal,
            secondarySuperscript = secVal.ifEmpty { null },
            backgroundColor = bg,
            valueTextColor = valColor,
            labelTextColor = lblColor,
            dividerColor = divColor,
            zoneColor = zoneColor,
            activeZoneIndex = activeIdx,
            isColorBackgroundActive = isBgActive,
            formattedZoneText = formattedZoneText,
        )
    }
}
