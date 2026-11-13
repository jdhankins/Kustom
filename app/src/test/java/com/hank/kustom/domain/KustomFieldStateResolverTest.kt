package com.hank.kustom.domain

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZonePalette
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KustomFieldStateResolverTest {

    @Test
    fun testAll68CatalogFieldsAreUniqueAndNonBlank() {
        val fields = KustomFieldCatalog.allFields
        assertEquals(85, fields.size)

        val ids = fields.map { it.typeId }
        assertEquals(ids.size, ids.distinct().size)

        fields.forEach { field ->
            assertTrue("Field ID should not be blank", field.typeId.isNotBlank())
            assertTrue("Field title should not be blank", field.childTitle.isNotBlank())
        }
    }

    @Test
    fun testSingleFieldStateResolutionForNegativeGrade() {
        val gradeDef = KustomFieldCatalog.findByTypeId("grade")!!
        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = gradeDef,
            streamState = StreamState.Streaming(io.hammerhead.karooext.models.DataPoint("grade", mapOf(DataType.Field.SINGLE to -5.4))),
            dynamicValue = -5.4,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
        )

        assertEquals("-5.4", state.rawValueText)
        assertEquals("-5", state.primaryValue)
        assertEquals("4", state.secondarySuperscript)
    }

    @Test
    fun testCoastingDownhillCadenceDisplaysZero() {
        val cadenceDef = KustomFieldCatalog.findByTypeId("cadence")!!

        // First, when not streamed yet and StreamState.NotAvailable -> "No Sensor"
        val notAvailableState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.NotAvailable,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = false,
            isInRide = false,
        )
        assertEquals("No Sensor", notAvailableState.rawValueText)

        // Coasting downhill: StreamState.Idle with hasEverStreamed = true -> "0"
        val idleCoastingState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.Idle,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )
        assertEquals("0", idleCoastingState.rawValueText)
    }

    @Test
    fun testSmoothedFractionalPowerZoneBoundaryMatching() {
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
                UserProfile.Zone(211, 250), // Zone 3
                UserProfile.Zone(251, 285), // Zone 4
                UserProfile.Zone(286, 325), // Zone 5
                UserProfile.Zone(326, 375), // Zone 6
                UserProfile.Zone(376, 500), // Zone 7
            ),
            heartRateZones = emptyList(),
        )

        // 164.6W rounds to 165W (Zone 2)
        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(io.hammerhead.karooext.models.DataPoint("power", mapOf(DataType.Field.SINGLE to 164.6))),
            dynamicValue = 164.6,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = userProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )

        assertEquals("165", state.rawValueText)
        assertEquals(1, state.activeZoneIndex) // 0-based index 1 = Zone 2
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], state.zoneColor) // Zone 2 Blue, NEVER Red!
    }

    @Test
    fun testVamMetersPerHourConversion() {
        val vamDef = KustomFieldCatalog.findByTypeId("vam")!!
        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = vamDef,
            streamState = StreamState.Streaming(io.hammerhead.karooext.models.DataPoint("vam", mapOf(DataType.Field.SINGLE to 0.25))),
            dynamicValue = 0.25,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
        )

        assertEquals("900", state.rawValueText)
    }

    @Test
    fun testSdkDataTypeStreamMappings() {
        assertEquals(DataType.Type.PEDAL_POWER_BALANCE, KustomFieldCatalog.POWER_BALANCE.resolveSdkDataTypeId(SmoothingOption.NONE))
        assertEquals(DataType.Type.SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE, KustomFieldCatalog.POWER_BALANCE.resolveSdkDataTypeId(SmoothingOption.THREE_SEC))
        assertEquals(DataType.Type.PEDAL_SMOOTHNESS, KustomFieldCatalog.POWER_SMOOTHNESS.resolveSdkDataTypeId(SmoothingOption.NONE))
        assertEquals(DataType.Type.TORQUE_EFFECTIVENESS, KustomFieldCatalog.POWER_TE.resolveSdkDataTypeId(SmoothingOption.NONE))
        assertEquals(DataType.Type.AVERAGE_TORQUE, KustomFieldCatalog.AVG_TORQUE.resolveSdkDataTypeId(SmoothingOption.NONE))
        assertEquals(DataType.Type.MAX_TORQUE, KustomFieldCatalog.MAX_TORQUE.resolveSdkDataTypeId(SmoothingOption.NONE))
        assertEquals(DataType.Type.ELEVATION_GAIN_LAP, KustomFieldCatalog.LAP_ASCENT.resolveSdkDataTypeId(SmoothingOption.NONE))
    }

    @Test
    fun testColorMatrixConfigurations() {
        val gradeDef = KustomFieldCatalog.findByTypeId("grade")!!

        // Day Mode, Background Off
        val dayBgOff = KustomFieldStateResolver.resolveSingleFieldState(
            definition = gradeDef,
            streamState = StreamState.Streaming(io.hammerhead.karooext.models.DataPoint("grade", mapOf(DataType.Field.SINGLE to 5.5))),
            dynamicValue = 5.5,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            isDarkTheme = false,
        )
        assertEquals(0xFFFFFFFF.toInt(), dayBgOff.backgroundColor)

        // Night Mode, Background Off
        val nightBgOff = KustomFieldStateResolver.resolveSingleFieldState(
            definition = gradeDef,
            streamState = StreamState.Streaming(io.hammerhead.karooext.models.DataPoint("grade", mapOf(DataType.Field.SINGLE to 5.5))),
            dynamicValue = 5.5,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            isDarkTheme = true,
        )
        assertEquals(0xFF000000.toInt(), nightBgOff.backgroundColor)
    }
}
