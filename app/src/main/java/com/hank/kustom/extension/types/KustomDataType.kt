package com.hank.kustom.extension.types

import android.content.Context
import android.widget.RemoteViews
import com.hank.kustom.R
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.data.repository.KarooSystemRepository
import com.hank.kustom.data.repository.KustomSettingsRepository
import com.hank.kustom.domain.KustomColorGradeMapper
import com.hank.kustom.domain.KustomFieldRenderer
import com.hank.kustom.domain.RollingTimeSmoother
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Unified [DataTypeImpl] supporting both Page Design / Idle mock animations and active in-ride telemetry.
 */
class KustomDataType(
    val definition: KustomFieldDefinition,
    private val karooSystemRepository: KarooSystemRepository,
    private val settingsRepository: KustomSettingsRepository,
) : DataTypeImpl("kustom", definition.typeId) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.Default).launch {
            combine(
                karooSystemRepository.rideStateFlow().onStart { emit(RideState.Idle) },
                settingsRepository.settingsFlow,
            ) { rideState, settings ->
                Pair(rideState, settings)
            }.flatMapLatest { (rideState, settings) ->
                val fieldConfig = settings.getFieldConfig(definition.typeId)
                val sdkTypeId = definition.resolveSdkDataTypeId(fieldConfig.smoothingOption)

                if (rideState is RideState.Recording) {
                    karooSystemRepository.streamDataFlow(sdkTypeId)
                } else {
                    flow {
                        var i = 0
                        while (true) {
                            val mockVal = definition.mockValues[i % definition.mockValues.size]
                            val dp = DataPoint(
                                dataTypeId = sdkTypeId,
                                values = mapOf(DataType.Field.SINGLE to mockVal),
                            )
                            emit(StreamState.Streaming(dp))
                            i++
                            delay(1200L)
                        }
                    }
                }
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
            var lastSmoothingSec = -1

            combine(
                karooSystemRepository.rideStateFlow().onStart { emit(RideState.Idle) },
                karooSystemRepository.userProfileFlow().onStart { emit(null) },
                settingsRepository.settingsFlow,
            ) { rideState, userProfile, settings ->
                Triple(rideState, userProfile, settings)
            }.flatMapLatest { (rideState, userProfile, settings) ->
                val fieldConfig = settings.getFieldConfig(definition.typeId)
                val isPageDesign = config.preview || rideState !is RideState.Recording

                if (isPageDesign) {
                    flow {
                        var i = 0
                        while (true) {
                            val mockVal = definition.mockValues[i % definition.mockValues.size]
                            emit(RenderPayload(mockVal, userProfile, settings, fieldConfig, isMock = true))
                            i++
                            delay(1200L)
                        }
                    }
                } else {
                    val sdkTypeId = definition.resolveSdkDataTypeId(fieldConfig.smoothingOption)
                    karooSystemRepository.streamDataFlow(sdkTypeId).map { streamState ->
                        val rawVal = (streamState as? StreamState.Streaming)?.dataPoint?.singleValue
                        RenderPayload(rawVal, userProfile, settings, fieldConfig, isMock = false)
                    }
                }
            }.collect { payload ->
                val (rawVal, userProfile, settings, fieldConfig, isMock) = payload

                if (fieldConfig.smoothingOption.seconds != lastSmoothingSec) {
                    lastSmoothingSec = fieldConfig.smoothingOption.seconds
                    gradeSmoother = RollingTimeSmoother(lastSmoothingSec)
                }

                val isImperial = userProfile?.preferredUnit?.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL

                val dynamicValue = if (rawVal != null) {
                    if (definition.typeId == "grade" && !isMock) {
                        gradeSmoother.addSample(rawVal)
                    } else {
                        rawVal
                    }
                } else {
                    null
                }

                val dynamicLabel = definition.resolveDynamicLabel(fieldConfig.smoothingOption)
                val valueText = if (dynamicValue != null) {
                    definition.formatNumericValueOnly(dynamicValue, isImperial)
                } else {
                    "--"
                }

                val mappedColor = computeMappedColor(
                    definition = definition,
                    rawVal = dynamicValue,
                    fieldConfig = fieldConfig,
                    userProfile = userProfile,
                )

                val bitmap = KustomFieldRenderer.render(
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
                )

                val remoteViews = RemoteViews(context.packageName, R.layout.kustom_remote_field_canvas).apply {
                    setImageViewBitmap(R.id.kustom_field_canvas_image, bitmap)
                }
                emitter.updateView(remoteViews)
            }
        }

        emitter.setCancellable { job.cancel() }
    }

    private data class RenderPayload(
        val rawValue: Double?,
        val userProfile: UserProfile?,
        val settings: KustomSettings,
        val fieldConfig: KustomFieldConfig,
        val isMock: Boolean,
    )

    private fun computeMappedColor(
        definition: KustomFieldDefinition,
        rawVal: Double?,
        fieldConfig: KustomFieldConfig,
        userProfile: UserProfile?,
    ): Int? {
        if (rawVal == null || !definition.supportsZones) return null

        return when (definition.typeId) {
            "power", "np", "avg-power", "max-power", "lap-power", "last-lap-power" -> {
                KustomColorGradeMapper.getPowerZoneColor(rawVal, userProfile, fieldConfig.colorMode)
            }
            "hr", "avg-hr", "max-hr", "lap-hr" -> {
                KustomColorGradeMapper.getHeartRateZoneColor(rawVal, userProfile, fieldConfig.colorMode)
            }
            "grade" -> {
                KustomColorGradeMapper.getGradeColor(rawVal, fieldConfig.colorMode)
            }
            else -> null
        }
    }
}
