package com.hank.kustom.domain

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.ZonePalette
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KustomFieldStateResolverTest {

    private val testUserProfile = UserProfile(
        preferredUnit = UserProfile.PreferredUnit(
            distance = UserProfile.PreferredUnit.UnitType.METRIC,
            elevation = UserProfile.PreferredUnit.UnitType.METRIC,
            temperature = UserProfile.PreferredUnit.UnitType.METRIC,
            weight = UserProfile.PreferredUnit.UnitType.METRIC,
        ),
        weight = 70.0f,
        ftp = 250,
        maxHr = 190,
        restingHr = 55,
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
            UserProfile.Zone(0, 114),   // Zone 1
            UserProfile.Zone(115, 133), // Zone 2
            UserProfile.Zone(134, 152), // Zone 3
            UserProfile.Zone(153, 171), // Zone 4
            UserProfile.Zone(172, 190), // Zone 5
        ),
    )

    @Test
    fun testAll85CatalogFieldsLabelsAndHeaderToggle() {
        val fields = KustomFieldCatalog.allFields
        assertEquals(85, fields.size)
        val ids = fields.map { it.typeId }
        assertEquals(ids.size, ids.distinct().size)

        fields.forEach { field ->
            assertTrue("Field ID should not be blank", field.typeId.isNotBlank())

            // 1. Header enabled across all SmoothingOptions
            SmoothingOption.entries.forEach { smoothing ->
                val stateWithHeader = KustomFieldStateResolver.resolveSingleFieldState(
                    definition = field,
                    streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to 150.0))),
                    dynamicValue = 150.0,
                    isImperial = false,
                    colorMode = ColorMode.NONE,
                    userProfile = testUserProfile,
                    globalColors = GlobalColorConfig(),
                    smoothing = smoothing,
                    showHeader = true,
                    isWorkoutActiveOnKaroo = true,
                )
                assertEquals(
                    "Label mismatch for ${field.typeId} with smoothing $smoothing",
                    field.resolveDynamicLabel(smoothing),
                    stateWithHeader.label
                )
                assertTrue(
                    "Label should not be blank for ${field.typeId}",
                    stateWithHeader.label.isNotBlank()
                )
            }

            // 2. Header disabled -> empty string label
            val stateWithoutHeader = KustomFieldStateResolver.resolveSingleFieldState(
                definition = field,
                streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to 150.0))),
                dynamicValue = 150.0,
                isImperial = false,
                colorMode = ColorMode.NONE,
                userProfile = testUserProfile,
                globalColors = GlobalColorConfig(),
                smoothing = SmoothingOption.THREE_SEC,
                showHeader = false,
                isWorkoutActiveOnKaroo = true,
            )
            assertEquals("Header off must yield empty label for ${field.typeId}", "", stateWithoutHeader.label)
        }
    }

    @Test
    fun testAll85CatalogFieldsValueFormattingMetricAndImperial() {
        val sampleValues = listOf(0.0, 12.3, 165.0, 245.8, -5.4)

        KustomFieldCatalog.allFields.forEach { field ->
            sampleValues.forEach { sample ->
                listOf(false, true).forEach { isImperial ->
                    val state = KustomFieldStateResolver.resolveSingleFieldState(
                        definition = field,
                        streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to sample))),
                        dynamicValue = sample,
                        isImperial = isImperial,
                        colorMode = ColorMode.NONE,
                        userProfile = testUserProfile,
                        globalColors = GlobalColorConfig(),
                        isWorkoutActiveOnKaroo = true,
                    )

                    assertTrue("rawValueText blank for ${field.typeId}", state.rawValueText.isNotBlank())
                    assertFalse("rawValueText NaN for ${field.typeId}", state.rawValueText.contains("NaN"))
                    assertFalse("rawValueText null for ${field.typeId}", state.rawValueText.contains("null"))
                    assertTrue("primaryValue blank for ${field.typeId}", state.primaryValue.isNotBlank())

                    if (!field.typeId.startsWith("workout-") || sample >= 0.0) {
                        assertEquals(
                            "Formatted value mismatch for ${field.typeId} (imperial=$isImperial, sample=$sample)",
                            field.formatNumericValueOnly(sample, isImperial),
                            state.rawValueText
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testAll85CatalogFieldsBackgroundColorsAndTextColorsAcrossThemesModesAndPalettes() {
        val zonedFieldIds = setOf(
            "power", "np", "avg-power", "max-power", "power-20m", "power-1hr",
            "power-combo", "custom-combo", "percent-ftp",
            "hr", "avg-hr", "max-hr", "hr-combo", "percent-max-hr", "percent-hrr",
            "grade"
        )

        KustomFieldCatalog.allFields.forEach { field ->
            ZonePalette.entries.forEach { palette ->
                val globalColors = GlobalColorConfig(globalZonePalette = palette)

                listOf(false, true).forEach { isDarkTheme ->
                    val expectedDefaultBg = if (isDarkTheme) globalColors.getDarkColorInt() else globalColors.getLightColorInt()
                    val expectedDefaultText = KustomColorGradeMapper.getContrastingTextColor(expectedDefaultBg)

                    // 1. ColorMode.NONE
                    val noneState = KustomFieldStateResolver.resolveSingleFieldState(
                        definition = field,
                        streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to 175.0))),
                        dynamicValue = 175.0,
                        isImperial = false,
                        colorMode = ColorMode.NONE,
                        userProfile = testUserProfile,
                        globalColors = globalColors,
                        isDarkTheme = isDarkTheme,
                        isWorkoutActiveOnKaroo = true,
                    )
                    assertEquals("BG mismatch in ColorMode.NONE for ${field.typeId}", expectedDefaultBg, noneState.backgroundColor)
                    assertEquals("Value text color mismatch in ColorMode.NONE for ${field.typeId}", expectedDefaultText, noneState.valueTextColor)
                    assertEquals("Label text color mismatch in ColorMode.NONE for ${field.typeId}", expectedDefaultText, noneState.labelTextColor)
                    assertFalse(noneState.isColorBackgroundActive)

                    // 2. ColorMode.TEXT_COLOR
                    val textColorState = KustomFieldStateResolver.resolveSingleFieldState(
                        definition = field,
                        streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to 175.0))),
                        dynamicValue = 175.0,
                        isImperial = false,
                        colorMode = ColorMode.TEXT_COLOR,
                        userProfile = testUserProfile,
                        globalColors = globalColors,
                        isDarkTheme = isDarkTheme,
                        isWorkoutActiveOnKaroo = true,
                    )
                    assertEquals("BG must stay theme default in TEXT_COLOR for ${field.typeId}", expectedDefaultBg, textColorState.backgroundColor)
                    assertEquals("Label must stay high-contrast default in TEXT_COLOR for ${field.typeId}", expectedDefaultText, textColorState.labelTextColor)
                    if (field.typeId in zonedFieldIds) {
                        assertNotNull("Expected zoneColor for ${field.typeId} in TEXT_COLOR", textColorState.zoneColor)
                        assertEquals("Value text must equal zoneColor for ${field.typeId}", textColorState.zoneColor, textColorState.valueTextColor)
                    } else {
                        assertNull("Non-zoned field ${field.typeId} should have null zoneColor", textColorState.zoneColor)
                        assertEquals("Non-zoned field ${field.typeId} should use default text color", expectedDefaultText, textColorState.valueTextColor)
                    }

                    // 3. ColorMode.BACKGROUND_COLOR
                    val bgColorState = KustomFieldStateResolver.resolveSingleFieldState(
                        definition = field,
                        streamState = StreamState.Streaming(DataPoint(field.typeId, mapOf(DataType.Field.SINGLE to 175.0))),
                        dynamicValue = 175.0,
                        isImperial = false,
                        colorMode = ColorMode.BACKGROUND_COLOR,
                        userProfile = testUserProfile,
                        globalColors = globalColors,
                        isDarkTheme = isDarkTheme,
                        isWorkoutActiveOnKaroo = true,
                    )
                    if (field.typeId in zonedFieldIds) {
                        assertTrue("isColorBackgroundActive must be true for ${field.typeId}", bgColorState.isColorBackgroundActive)
                        assertEquals("BG must equal zoneColor for ${field.typeId}", bgColorState.zoneColor, bgColorState.backgroundColor)
                        val expectedContrast = KustomColorGradeMapper.getContrastingTextColor(bgColorState.backgroundColor)
                        assertEquals("Contrasting value text mismatch on colored BG for ${field.typeId}", expectedContrast, bgColorState.valueTextColor)
                        assertEquals("Contrasting label text mismatch on colored BG for ${field.typeId}", expectedContrast, bgColorState.labelTextColor)
                    } else {
                        assertFalse("isColorBackgroundActive must be false for non-zoned ${field.typeId}", bgColorState.isColorBackgroundActive)
                        assertEquals("BG must stay theme default for non-zoned ${field.typeId}", expectedDefaultBg, bgColorState.backgroundColor)
                    }
                }
            }
        }
    }

    @Test
    fun testSingleFieldStateResolutionForNegativeGrade() {
        val gradeDef = KustomFieldCatalog.findByTypeId("grade")!!
        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = gradeDef,
            streamState = StreamState.Streaming(DataPoint("grade", mapOf(DataType.Field.SINGLE to -5.4))),
            dynamicValue = -5.4,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )

        assertEquals(gradeDef.resolveDynamicLabel(SmoothingOption.NONE), state.label)
        assertEquals("-5.4", state.rawValueText)
        assertEquals("-5", state.primaryValue)
        assertEquals("4", state.secondarySuperscript)
        assertNotNull("Grade in BACKGROUND_COLOR mode must resolve a grade color", state.zoneColor)
        assertEquals(state.zoneColor, state.backgroundColor)
    }

    @Test
    fun testCoastingDownhillCadenceAndStreamStates() {
        val cadenceDef = KustomFieldCatalog.findByTypeId("cadence")!!

        // 1. NotAvailable before ever streaming -> "No Sensor"
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

        // 2. Searching before ever streaming -> "Searching..." (never split by '.')
        val searchingState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.Searching,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = false,
            isInRide = false,
        )
        assertEquals("Searching...", searchingState.rawValueText)
        assertEquals("Searching...", searchingState.primaryValue)
        assertNull(searchingState.secondarySuperscript)

        // 3. StreamState.Idle even when hasEverStreamed = false & isInRide = false -> "0"
        val idleState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.Idle,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = false,
            isInRide = false,
        )
        assertEquals("0", idleState.rawValueText)

        // 4. Temporary NotAvailable dropout while coasting downhill during active ride -> "0"
        val coastingDropoutState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = cadenceDef,
            streamState = StreamState.NotAvailable,
            dynamicValue = null,
            isImperial = false,
            colorMode = ColorMode.NONE,
            userProfile = null,
            globalColors = GlobalColorConfig(),
            hasEverStreamed = true,
            isInRide = true,
        )
        assertEquals("0", coastingDropoutState.rawValueText)
    }

    @Test
    fun testSmoothedFractionalPowerAndHrZoneBoundariesAndAboveMaxSprint() {
        val powerDef = KustomFieldCatalog.findByTypeId("power")!!
        val hrDef = KustomFieldCatalog.findByTypeId("hr")!!

        // 164.6W rounds to 165W -> Zone 2 (index 1)
        val z2PowerState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 164.6))),
            dynamicValue = 164.6,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )
        assertEquals("165", z2PowerState.rawValueText)
        assertEquals(1, z2PowerState.activeZoneIndex)
        assertEquals("Z2", z2PowerState.formattedZoneText)
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], z2PowerState.zoneColor)
        assertEquals(ZonePalette.KAROO.get7ZoneColors()[1], z2PowerState.backgroundColor)

        // 650W sprint above Zone 7 max (500W) -> clamps to Zone 7 (index 6), never -1
        val sprintPowerState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = powerDef,
            streamState = StreamState.Streaming(DataPoint("power", mapOf(DataType.Field.SINGLE to 650.0))),
            dynamicValue = 650.0,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )
        assertEquals("650", sprintPowerState.rawValueText)
        assertEquals(6, sprintPowerState.activeZoneIndex)
        assertEquals("Z7", sprintPowerState.formattedZoneText)

        // 133.6 bpm rounds to 134 bpm -> HR Zone 3 (index 2)
        val z3HrState = KustomFieldStateResolver.resolveSingleFieldState(
            definition = hrDef,
            streamState = StreamState.Streaming(DataPoint("hr", mapOf(DataType.Field.SINGLE to 133.6))),
            dynamicValue = 133.6,
            isImperial = false,
            colorMode = ColorMode.BACKGROUND_COLOR,
            userProfile = testUserProfile,
            globalColors = GlobalColorConfig(globalZonePalette = ZonePalette.KAROO),
        )
        assertEquals("134", z3HrState.rawValueText)
        assertEquals(2, z3HrState.activeZoneIndex)
        assertEquals("Z3", z3HrState.formattedZoneText)
        assertNotNull(z3HrState.zoneColor)
        assertEquals(z3HrState.zoneColor, z3HrState.backgroundColor)
    }

    @Test
    fun testVamMetersPerHourConversion() {
        val vamDef = KustomFieldCatalog.findByTypeId("vam")!!
        val state = KustomFieldStateResolver.resolveSingleFieldState(
            definition = vamDef,
            streamState = StreamState.Streaming(DataPoint("vam", mapOf(DataType.Field.SINGLE to 0.25))),
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
}
