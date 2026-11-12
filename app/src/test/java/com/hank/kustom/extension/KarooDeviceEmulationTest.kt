package com.hank.kustom.extension

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.ZonePalette
import com.hank.kustom.domain.KustomFieldStateResolver
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KarooDeviceEmulationTest {

    @Test
    fun testEmulatedKarooDeviceSingleMetricFieldStateResolution() {
        val powerDef = KustomFieldCatalog.findByTypeId("power")!!
        val userProfile = UserProfile(
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
            ),
            heartRateZones = emptyList(),
        )

        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 164.6))),
            dynamicValue = 164.6,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = userProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )

        assertEquals("165", state.rawValueText)
        assertEquals("165", state.primaryValue)
        assertEquals(1, state.activeZoneIndex)
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], state.zoneColor)
    }

    @Test
    fun testEmulatedKarooDeviceComboBlockStateResolution() {
        val centerDef = KustomFieldCatalog.findByTypeId("power")!!
        val leftDef = KustomFieldCatalog.findByTypeId("avg-power")!!
        val rightDef = KustomFieldCatalog.findByTypeId("max-power")!!

        val centerState = KustomFieldStateResolver.resolveValueString(
            definition = centerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 242.0))),
            dynamicValue = 242.0,
            isImperial = false,
        )

        val leftState = KustomFieldStateResolver.resolveValueString(
            definition = leftDef,
            streamState = StreamState.Streaming(DataPoint("avg-power", mapOf(DataType.Field.SINGLE to 218.0))),
            dynamicValue = 218.0,
            isImperial = false,
        )

        val rightState = KustomFieldStateResolver.resolveValueString(
            definition = rightDef,
            streamState = StreamState.Streaming(DataPoint("max-power", mapOf(DataType.Field.SINGLE to 680.0))),
            dynamicValue = 680.0,
            isImperial = false,
        )

        assertEquals("242", centerState)
        assertEquals("218", leftState)
        assertEquals("680", rightState)
    }
}
