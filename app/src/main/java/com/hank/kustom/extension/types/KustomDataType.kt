package com.hank.kustom.extension.types

import android.content.Context
import android.widget.RemoteViews
import com.hank.kustom.R
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.data.model.ZonePalette
import com.hank.kustom.data.repository.KarooSystemRepository
import com.hank.kustom.data.repository.KustomSettingsRepository
import com.hank.kustom.domain.KustomColorGradeMapper
import com.hank.kustom.domain.KustomFieldRenderer
import com.hank.kustom.domain.RollingTimeSmoother
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Unified [DataTypeImpl] supporting both Page Design / Idle mock animations and active in-ride telemetry with multi-stream corner metrics.
 */
class KustomDataType(
    val definition: KustomFieldDefinition,
    private val karooSystemRepository: KarooSystemRepository,
    private val settingsRepository: KustomSettingsRepository,
) : DataTypeImpl("kustom", definition.typeId) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.Default).launch {
            settingsRepository.settingsFlow.flatMapLatest { settings ->
                val fieldConfig = settings.getFieldConfig(definition.typeId)
                val sdkTypeId = definition.resolveSdkDataTypeId(fieldConfig.smoothingOption)
                karooSystemRepository.streamDataFlow(sdkTypeId)
            }.collect { streamState ->
                emitter.onNext(streamState)
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val job = CoroutineScope(Dispatchers.Default).launch {
            var gradeSmoother = RollingTimeSmoother(3)
            var centerGradeSmoother = RollingTimeSmoother(3)
            var leftGradeSmoother = RollingTimeSmoother(3)
            var rightGradeSmoother = RollingTimeSmoother(3)
            var lastSmoothingSec = -1

            combine(
                karooSystemRepository.rideStateFlow().onStart { emit(RideState.Idle) },
                karooSystemRepository.userProfileFlow().onStart { emit(null) },
                settingsRepository.settingsFlow,
            ) { rideState, userProfile, settings ->
                Triple(rideState, userProfile, settings)
            }.flatMapLatest { (rideState, userProfile, settings) ->
                val fieldConfig = settings.getFieldConfig(definition.typeId)
                val isInRide = rideState is RideState.Recording || rideState is RideState.Paused
                val isMockMode = config.preview && !isInRide

                val customComboInst = settings.customComboInstances.firstOrNull { it.id == definition.typeId }
                val isComboBlock = customComboInst != null || definition.typeId.startsWith("combo-block-") || definition.typeId == "power-combo" || definition.typeId == "custom-combo"

                if (isMockMode) {
                    // Page Setup / Profile Design Mode Only (when not in ride): Loop animated mock values
                    flow {
                        var i = 0
                        while (true) {
                            val mockVal = definition.mockValues[i % definition.mockValues.size]
                            val mockState = StreamState.Streaming(
                                DataPoint(definition.typeId, mapOf(DataType.Field.SINGLE to mockVal))
                            )
                            emit(
                                RenderPayload(
                                    streamState = mockState,
                                    userProfile = userProfile,
                                    settings = settings,
                                    fieldConfig = fieldConfig,
                                    isMock = true,
                                    mockValue = mockVal,
                                    targetDataPoint = DataPoint(
                                        dataTypeId = DataType.Type.WORKOUT_PRIMARY_TARGET,
                                        values = mapOf(
                                            DataType.Field.WORKOUT_TARGET_MIN_VALUE to 117.0,
                                            DataType.Field.WORKOUT_TARGET_MAX_VALUE to 146.0,
                                        ),
                                    ),
                                )
                            )
                            i++
                            delay(1200L)
                        }
                    }
                } else if (isComboBlock) {
                    // 3-in-1 Combo Block Mode: Concurrently stream live telemetry for Center, Left, and Right metrics
                    val resolvedConfig = if (customComboInst != null) {
                        KustomFieldConfig(
                            smoothingOption = customComboInst.centerSmoothing,
                            zoneFormat = customComboInst.centerZoneFormat,
                            comboCenterMetric = customComboInst.centerMetric,
                            comboLeftMetric = customComboInst.leftMetric,
                            comboRightMetric = customComboInst.rightMetric,
                            comboLeftSmoothing = customComboInst.leftSmoothing,
                            comboRightSmoothing = customComboInst.rightSmoothing,
                            comboLeftZoneFormat = customComboInst.leftZoneFormat,
                            comboRightZoneFormat = customComboInst.rightZoneFormat,
                        )
                    } else {
                        fieldConfig
                    }

                    val centerDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboCenterMetric) ?: KustomFieldCatalog.POWER
                    val leftDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboLeftMetric) ?: KustomFieldCatalog.NP
                    val rightDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboRightMetric) ?: KustomFieldCatalog.MAX_POWER

                    val centerSdkId = centerDef.resolveSdkDataTypeId(resolvedConfig.smoothingOption)
                    val leftSdkId = leftDef.resolveSdkDataTypeId(resolvedConfig.comboLeftSmoothing)
                    val rightSdkId = rightDef.resolveSdkDataTypeId(resolvedConfig.comboRightSmoothing)

                    val centerFlow = karooSystemRepository.streamDataFlow(centerSdkId)
                    val leftFlow = if (resolvedConfig.comboLeftMetric == "none") flowOf(StreamState.NotAvailable) else karooSystemRepository.streamDataFlow(leftSdkId)
                    val rightFlow = if (resolvedConfig.comboRightMetric == "none") flowOf(StreamState.NotAvailable) else karooSystemRepository.streamDataFlow(rightSdkId)

                    combine(centerFlow, leftFlow, rightFlow) { centerState, leftState, rightState ->
                        RenderPayload(
                            streamState = centerState,
                            leftStreamState = leftState,
                            rightStreamState = rightState,
                            userProfile = userProfile,
                            settings = settings,
                            fieldConfig = resolvedConfig,
                            isMock = false,
                        )
                    }
                } else if (definition.typeId == "workout-target-power") {
                    // Workout Power: Stream live current power AND live workout target streams
                    val sdkTypeId = definition.resolveSdkDataTypeId(fieldConfig.smoothingOption)
                    combine(
                        karooSystemRepository.streamDataFlow(sdkTypeId),
                        karooSystemRepository.streamDataFlow(DataType.Type.WORKOUT_POWER_TARGET),
                        karooSystemRepository.streamDataFlow(DataType.Type.WORKOUT_PRIMARY_TARGET),
                    ) { powerState, targetPowerState, primaryTargetState ->
                        val pDp = (targetPowerState as? StreamState.Streaming)?.dataPoint
                        val priDp = (primaryTargetState as? StreamState.Streaming)?.dataPoint
                        val targetDp = priDp ?: pDp
                        RenderPayload(
                            streamState = powerState,
                            userProfile = userProfile,
                            settings = settings,
                            fieldConfig = fieldConfig,
                            isMock = false,
                            targetDataPoint = targetDp,
                        )
                    }
                } else {
                    // Active Ride Page Mode: Stream live sensor telemetry states from Karoo OS for standard single-metric fields
                    val sdkTypeId = definition.resolveSdkDataTypeId(fieldConfig.smoothingOption)
                    karooSystemRepository.streamDataFlow(sdkTypeId).map { streamState ->
                        RenderPayload(
                            streamState = streamState,
                            userProfile = userProfile,
                            settings = settings,
                            fieldConfig = fieldConfig,
                            isMock = false,
                        )
                    }
                }
            }.collect { payload ->
                val (streamState, leftStreamState, rightStreamState, userProfile, settings, fieldConfig, isMock, mockValue, targetDataPoint) = payload

                if (fieldConfig.smoothingOption.seconds != lastSmoothingSec) {
                    lastSmoothingSec = fieldConfig.smoothingOption.seconds
                    gradeSmoother = RollingTimeSmoother(lastSmoothingSec)
                    centerGradeSmoother = RollingTimeSmoother(lastSmoothingSec)
                    leftGradeSmoother = RollingTimeSmoother(fieldConfig.comboLeftSmoothing.seconds)
                    rightGradeSmoother = RollingTimeSmoother(fieldConfig.comboRightSmoothing.seconds)
                }

                val isImperial = userProfile?.preferredUnit?.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL

                val dynamicValue: Double? = if (isMock) {
                    mockValue
                } else if (streamState is StreamState.Streaming) {
                    val rawVal = streamState.dataPoint.singleValue
                    if (rawVal != null && definition.typeId == "grade") {
                        gradeSmoother.addSample(rawVal)
                    } else {
                        rawVal
                    }
                } else {
                    null
                }

                val dynamicLabel = if (definition.typeId == "workout-target-power") {
                    val targetText = extractTargetPowerString(targetDataPoint)
                    targetText ?: "Target Power"
                } else {
                    definition.resolveDynamicLabel(fieldConfig.smoothingOption)
                }

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

                val valueText = when {
                    isMock -> {
                        val v = dynamicValue ?: 228.0
                        definition.formatNumericValueOnly(v, isImperial)
                    }
                    streamState is StreamState.Streaming -> {
                        val v = dynamicValue
                        if (v != null) {
                            definition.formatNumericValueOnly(v, isImperial)
                        } else if (isSensorField) {
                            "Searching..."
                        } else {
                            definition.formatNumericValueOnly(0.0, isImperial)
                        }
                    }
                    streamState is StreamState.Searching -> {
                        if (isSensorField) "Searching..." else definition.formatNumericValueOnly(0.0, isImperial)
                    }
                    streamState is StreamState.NotAvailable || streamState is StreamState.Idle -> {
                        if (isSensorField) "No Sensor" else definition.formatNumericValueOnly(0.0, isImperial)
                    }
                    else -> {
                        if (isSensorField) "No Sensor" else "--"
                    }
                }

                val mappedColor = computeMappedColor(
                    definition = definition,
                    rawVal = dynamicValue,
                    fieldConfig = fieldConfig,
                    userProfile = userProfile,
                    palette = settings.globalColors.globalZonePalette,
                )

                val isDarkTheme = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

                val customComboInst = settings.customComboInstances.firstOrNull { it.id == definition.typeId }
                val isComboBlock = customComboInst != null || definition.typeId.startsWith("combo-block-") || definition.typeId == "power-combo" || definition.typeId == "custom-combo"

                val bitmap = if (definition.typeId == "workout-graph") {
                    KustomFieldRenderer.renderWorkoutGraph(
                        context = context,
                        config = config,
                        showPowerTrace = fieldConfig.showPowerTrace,
                        showHrTrace = fieldConfig.showHrTrace,
                        showCadenceTrace = fieldConfig.showCadenceTrace,
                        isDarkTheme = isDarkTheme,
                        globalColors = settings.globalColors,
                    )
                } else if (isComboBlock) {
                    val resolvedConfig = if (customComboInst != null) {
                        KustomFieldConfig(
                            smoothingOption = customComboInst.centerSmoothing,
                            zoneFormat = customComboInst.centerZoneFormat,
                            comboCenterMetric = customComboInst.centerMetric,
                            comboLeftMetric = customComboInst.leftMetric,
                            comboRightMetric = customComboInst.rightMetric,
                            comboLeftSmoothing = customComboInst.leftSmoothing,
                            comboRightSmoothing = customComboInst.rightSmoothing,
                            comboLeftZoneFormat = customComboInst.leftZoneFormat,
                            comboRightZoneFormat = customComboInst.rightZoneFormat,
                        )
                    } else {
                        fieldConfig
                    }

                    val centerDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboCenterMetric) ?: KustomFieldCatalog.POWER
                    val leftDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboLeftMetric) ?: KustomFieldCatalog.NP
                    val rightDef = KustomFieldCatalog.findByTypeId(resolvedConfig.comboRightMetric) ?: KustomFieldCatalog.MAX_POWER

                    // Real live multi-stream values for Center, Left, and Right slots
                    val centerVal: Double? = if (isMock) {
                        centerDef.mockValues.first()
                    } else if (streamState is StreamState.Streaming) {
                        val rawVal = streamState.dataPoint.singleValue
                        if (rawVal != null && centerDef.typeId == "grade") {
                            centerGradeSmoother.addSample(rawVal)
                        } else {
                            rawVal
                        }
                    } else {
                        null
                    }

                    val leftVal: Double? = if (isMock) {
                        leftDef.mockValues.firstOrNull() ?: 203.0
                    } else if (leftStreamState is StreamState.Streaming) {
                        val rawVal = leftStreamState.dataPoint.singleValue
                        if (rawVal != null && leftDef.typeId == "grade") {
                            leftGradeSmoother.addSample(rawVal)
                        } else {
                            rawVal
                        }
                    } else {
                        null
                    }

                    val rightVal: Double? = if (isMock) {
                        rightDef.mockValues.firstOrNull() ?: 224.0
                    } else if (rightStreamState is StreamState.Streaming) {
                        val rawVal = rightStreamState.dataPoint.singleValue
                        if (rawVal != null && rightDef.typeId == "grade") {
                            rightGradeSmoother.addSample(rawVal)
                        } else {
                            rawVal
                        }
                    } else {
                        null
                    }

                    val primaryValText = if (centerVal == null) {
                        val isCenterSensor = when (centerDef.typeId) {
                            "power", "np", "avg-power", "max-power", "power-20m", "power-1hr", "power-balance",
                            "percent-ftp", "power-to-weight", "power-zone", "intensity-factor", "tss", "power-vi",
                            "power-kj", "power-cal", "power-cal-hr", "power-smoothness", "power-te", "avg-power-balance",
                            "hr", "avg-hr", "max-hr", "hr-zone", "percent-max-hr", "percent-hrr", "avg-percent-hrr",
                            "cadence", "torque", "avg-cadence", "max-cadence", "avg-torque", "max-torque" -> true
                            else -> false
                        }
                        if (isCenterSensor) "No Sensor" else "--"
                    } else if (centerDef.typeId.contains("hr") || centerDef.typeId.contains("cadence") || centerDef.typeId.contains("power")) {
                        "${centerVal.toInt()}"
                    } else {
                        centerDef.formatNumericValueOnly(centerVal, isImperial)
                    }

                    val leftValueFormattedText = when {
                        resolvedConfig.comboLeftMetric == "none" -> ""
                        leftVal != null -> leftDef.formatNumericValueOnly(leftVal, isImperial)
                        isMock -> leftDef.formatNumericValueOnly(leftDef.mockValues.firstOrNull() ?: 203.0, isImperial)
                        else -> "--"
                    }

                    val rightValueFormattedText = when {
                        resolvedConfig.comboRightMetric == "none" -> ""
                        rightVal != null -> rightDef.formatNumericValueOnly(rightVal, isImperial)
                        isMock -> rightDef.formatNumericValueOnly(rightDef.mockValues.firstOrNull() ?: 224.0, isImperial)
                        else -> "--"
                    }

                    val comboMappedColor = if (centerDef.supportsZones && centerVal != null) {
                        computeMappedColor(
                            definition = centerDef,
                            rawVal = centerVal,
                            fieldConfig = resolvedConfig,
                            userProfile = userProfile,
                            palette = settings.globalColors.globalZonePalette,
                        )
                    } else null

                    val leftZoneFormat = resolvedConfig.comboLeftZoneFormat
                    val leftMappedColor = if (leftDef.supportsZones && leftZoneFormat != ZoneFormat.NONE && leftVal != null) {
                        computeMappedColor(
                            definition = leftDef,
                            rawVal = leftVal,
                            fieldConfig = resolvedConfig.copy(zoneFormat = leftZoneFormat),
                            userProfile = userProfile,
                            palette = settings.globalColors.globalZonePalette,
                        )
                    } else null

                    val rightZoneFormat = resolvedConfig.comboRightZoneFormat
                    val rightMappedColor = if (rightDef.supportsZones && rightZoneFormat != ZoneFormat.NONE && rightVal != null) {
                        computeMappedColor(
                            definition = rightDef,
                            rawVal = rightVal,
                            fieldConfig = resolvedConfig.copy(zoneFormat = rightZoneFormat),
                            userProfile = userProfile,
                            palette = settings.globalColors.globalZonePalette,
                        )
                    } else null

                    KustomFieldRenderer.renderComboBlock(
                        context = context,
                        config = config,
                        label = centerDef.resolveDynamicLabel(resolvedConfig.smoothingOption),
                        primaryValueText = primaryValText,
                        smoothingLabel = "",
                        leftLabel = if (resolvedConfig.comboLeftMetric == "none") "" else leftDef.childTitle,
                        leftValueText = leftValueFormattedText,
                        rightLabel = if (resolvedConfig.comboRightMetric == "none") "" else rightDef.childTitle,
                        rightValueText = rightValueFormattedText,
                        powerValueWatts = centerVal,
                        ftpWatts = userProfile?.ftp?.toDouble() ?: 250.0,
                        zoneCount = settings.globalColors.globalPowerZoneCount,
                        zoneFormat = if (centerDef.supportsZones) resolvedConfig.zoneFormat else ZoneFormat.NONE,
                        mappedColor = comboMappedColor,
                        leftZoneFormat = leftZoneFormat,
                        leftMappedColor = leftMappedColor,
                        rightZoneFormat = rightZoneFormat,
                        rightMappedColor = rightMappedColor,
                        labelTypography = TypographyConfig(
                            font = settings.globalTypography.fontFamily,
                            fontWeight = settings.globalTypography.labelWeight,
                            fontSizeSp = settings.globalTypography.labelSizeSp.toFloat(),
                        ),
                        valueTypography = TypographyConfig(
                            font = settings.globalTypography.fontFamily,
                            fontWeight = settings.globalTypography.valueWeight,
                            fontSizeSp = settings.globalTypography.valueSizeSp.toFloat(),
                        ),
                        isDarkTheme = isDarkTheme,
                        globalColors = settings.globalColors,
                    )
                } else {
                    KustomFieldRenderer.render(
                        context = context,
                        config = config,
                        label = dynamicLabel,
                        valueText = valueText,
                        labelTypography = TypographyConfig(
                            font = settings.globalTypography.fontFamily,
                            fontWeight = settings.globalTypography.labelWeight,
                            fontSizeSp = settings.globalTypography.labelSizeSp.toFloat(),
                        ),
                        valueTypography = TypographyConfig(
                            font = settings.globalTypography.fontFamily,
                            fontWeight = settings.globalTypography.valueWeight,
                            fontSizeSp = settings.globalTypography.valueSizeSp.toFloat(),
                        ),
                        colorMode = fieldConfig.colorMode,
                        mappedColor = mappedColor,
                        isDarkTheme = isDarkTheme,
                        globalColors = settings.globalColors,
                    )
                }

                val remoteViews = RemoteViews(context.packageName, R.layout.kustom_remote_field_canvas).apply {
                    setImageViewBitmap(R.id.kustom_field_canvas_image, bitmap)
                }
                emitter.updateView(remoteViews)
            }
        }

        emitter.setCancellable { job.cancel() }
    }

    private data class RenderPayload(
        val streamState: StreamState?,
        val leftStreamState: StreamState? = null,
        val rightStreamState: StreamState? = null,
        val userProfile: UserProfile?,
        val settings: KustomSettings,
        val fieldConfig: KustomFieldConfig,
        val isMock: Boolean,
        val mockValue: Double? = null,
        val targetDataPoint: DataPoint? = null,
    )

    private fun extractTargetPowerString(dataPoint: DataPoint?): String? {
        if (dataPoint == null) return null
        KustomLogger.d("extractTargetPowerString dataPoint values: ${dataPoint.values}")

        var minVal: Double? = null
        var maxVal: Double? = null
        var targetVal: Double? = null

        for ((key, value) in dataPoint.values) {
            when {
                key.contains("MIN", ignoreCase = true) -> minVal = value
                key.contains("MAX", ignoreCase = true) -> maxVal = value
                key.contains("TARGET_VALUE", ignoreCase = true) || key.contains("OUTPUT", ignoreCase = true) -> {
                    if (targetVal == null || targetVal == 0.0) targetVal = value
                }
                else -> {
                    if (targetVal == null && value > 10.0) targetVal = value
                }
            }
        }

        KustomLogger.d("Extracted target watts: minVal=$minVal, maxVal=$maxVal, targetVal=$targetVal")

        return when {
            minVal != null && maxVal != null && minVal > 0 && maxVal > 0 && minVal != maxVal -> {
                "${minVal.toInt()}-${maxVal.toInt()}W"
            }
            targetVal != null && targetVal > 0 -> {
                "${targetVal.toInt()}W"
            }
            minVal != null && minVal > 0 -> {
                "${minVal.toInt()}W"
            }
            else -> null
        }
    }

    private fun computeMappedColor(
        definition: KustomFieldDefinition,
        rawVal: Double?,
        fieldConfig: KustomFieldConfig,
        userProfile: UserProfile?,
        palette: ZonePalette = ZonePalette.KAROO,
    ): Int? {
        if (rawVal == null || !definition.supportsZones) return null

        return when (definition.typeId) {
            "power", "power-combo", "np", "avg-power", "max-power", "lap-power", "last-lap-power", "percent-ftp", "power-balance", "power-zone", "power-to-weight", "workout-target-power", "workout-primary-target" -> {
                KustomColorGradeMapper.getPowerZoneColor(rawVal, userProfile, fieldConfig.colorMode, palette = palette)
            }
            "hr", "avg-hr", "max-hr", "lap-hr", "percent-max-hr", "hr-zone" -> {
                KustomColorGradeMapper.getHeartRateZoneColor(rawVal, userProfile, fieldConfig.colorMode, palette = palette)
            }
            "grade" -> {
                KustomColorGradeMapper.getGradeColor(rawVal, fieldConfig.colorMode)
            }
            else -> null
        }
    }
}
