package com.hank.kustom.extension

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZonePalette
import com.hank.kustom.domain.KustomColorGradeMapper
import com.hank.kustom.domain.KustomFieldStateResolver
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KarooDeviceEmulationTest {

    private val testUserProfile = UserProfile(
        preferredUnit = UserProfile.PreferredUnit(
            distance = UserProfile.PreferredUnit.UnitType.METRIC,
            elevation = UserProfile.PreferredUnit.UnitType.METRIC,
            temperature = UserProfile.PreferredUnit.UnitType.METRIC,
            weight = UserProfile.PreferredUnit.UnitType.METRIC,
        ),
        weight = 70.0f,
        ftp = 250,
        maxHr = 185,
        restingHr = 60,
        powerZones = listOf(
            UserProfile.Zone(0, 164),   // Zone 1
            UserProfile.Zone(165, 210), // Zone 2
            UserProfile.Zone(211, 250), // Zone 3
            UserProfile.Zone(251, 285), // Zone 4
            UserProfile.Zone(286, 325), // Zone 5
            UserProfile.Zone(326, 375), // Zone 6
            UserProfile.Zone(376, 500), // Zone 7
        ),
        heartRateZones = listOf(
            UserProfile.Zone(0, 115),
            UserProfile.Zone(116, 135),
            UserProfile.Zone(136, 155),
            UserProfile.Zone(156, 175),
            UserProfile.Zone(176, 195),
        ),
    )

    @Test
    fun testEmulatedKarooSingleFieldStateResolution() {
        val powerDef = KustomFieldCatalog.findByTypeId("power")!!

        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 164.6))),
            dynamicValue = 164.6,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
            smoothing = SmoothingOption.THREE_SEC,
            showHeader = true,
        )

        assertEquals(powerDef.resolveDynamicLabel(SmoothingOption.THREE_SEC), state.label)
        assertEquals("165", state.rawValueText)
        assertEquals("165", state.primaryValue)
        assertEquals(1, state.activeZoneIndex)
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], state.zoneColor)
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], state.backgroundColor)
        assertEquals(
            KustomColorGradeMapper.getContrastingTextColor(state.backgroundColor),
            state.valueTextColor
        )
    }

    @Test
    fun testEmulatedDownhillRideSequence_PedalingToCoastingAndNegativeGrade() {
        val cadenceDef = KustomFieldCatalog.findByTypeId("cadence")!!
        val gradeDef = KustomFieldCatalog.findByTypeId("grade")!!
        val powerDef = KustomFieldCatalog.findByTypeId("power")!!

        // Phase 1: Pedaling on flat (92 RPM, 220W, 0.0% grade)
        val pedalingCadence = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.Streaming(DataPoint("cadence", mapOf(DataType.Field.SINGLE to 92.0))),
            dynamicValue = 92.0,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )
        assertEquals("92", pedalingCadence.rawValueText)

        // Phase 2: Coasting downhill (Cadence -> StreamState.Idle with null value, Power -> 0W, Grade -> -6.8%)
        val coastingCadence = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.Idle,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )
        val coastingPower = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 0.0))),
            dynamicValue = 0.0,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )
        val downhillGrade = KustomFieldStateResolver.resolveSingleFieldState(
            definition = gradeDef,
            streamState = StreamState.Streaming(DataPoint("grade", mapOf(DataType.Field.SINGLE to -6.8))),
            dynamicValue = -6.8,
            isImperial = false,
            colorMode = ColorMode.TEXT_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )

        assertEquals("0", coastingCadence.rawValueText)
        assertEquals("0", coastingPower.rawValueText)
        assertEquals("-6.8", downhillGrade.rawValueText)
        assertEquals("-6", downhillGrade.primaryValue)
        assertEquals("8", downhillGrade.secondarySuperscript)
        assertNotNull(downhillGrade.zoneColor)
        assertEquals(downhillGrade.zoneColor, downhillGrade.valueTextColor)
    }
}
