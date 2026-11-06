# Project Rules: Kustom (com.hank.kustom) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: com.hank.kustom. Extension ID: "kustom".
- Primary extension service: com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1").
- Key architecture layers:
  - data/model/: KustomSettings, KustomFieldCatalog, KustomFieldConfig, CustomComboInstance (5 static 3-in-1 blocks: Block 1 Power, Block 2 HR, Block 3 Grade, Block 4 Workout, Block 5 Cadence), GlobalTypographyConfig, GlobalColorConfig, ZonePalette (including NEON_GLASS "Neon Glass"), SmoothingOption, ZoneFormat, KustomFont.
  - data/repository/: KarooSystemRepository and KustomSettingsRepository (Preferences DataStore "kustom_settings").
  - domain/: FullWidthLayoutEngine (centralized full-width slot layout engine), KustomFieldRenderer (single fields, 3-in-1 combo blocks, target power corridor, workout graph HUD, and 2-column debug terminal), WorkoutFileParser (local .fit and .zwo parser), KustomColorAndSmoothing, KustomTypefaceResolver.
  - extension/: KustomExtension and types/KustomDataType.
  - ui/: MainActivity and KustomSettingsViewModel.
- CRITICAL app/build.gradle.kts SDK Levels:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Keep minSdk = 26 and targetSdk = 34 (with compileSdk = 35 or release(37)).

## 2. TELEMETRY UNIT CONVERSIONS & SUPERSCRIPT FORMATTING RULES
- VAM Unit Conversion (m/s to m/h in KustomFieldCatalog.kt):
  - Karoo OS streams DataType.Type.VERTICAL_SPEED, AVERAGE_VERTICAL_SPEED_30S ("vam"), and AVERAGE_VERTICAL_SPEED ("avg-vam") in meters per second (m/s).
  - In KustomFieldCatalog.formatNumericValueOnly, "vam" and "avg-vam" MUST multiply rawValue (m/s) by 3600.0 to convert to vertical meters per hour (m/h): (rawValue * 3600.0).roundToInt().coerceAtLeast(0).toString().
- Negative Number Superscript Eligibility (FullWidthLayoutEngine.kt & KustomFieldRenderer.kt):
  - When checking if a decimal or time string is eligible for superscript formatting (e.g., "-3.4" -> "-3" with superscript "4"), check !renderText.drop(1).contains("-") instead of !renderText.contains("-"). A leading minus sign at index 0 MUST use superscript formatting, while range strings with an interior hyphen ("115-160") are excluded.

## 3. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- UNIFIED HEADER LABEL TYPOGRAPHY & OUTDOOR VISIBILITY (Left, Middle, and Right Labels):
  - Left (leftSlot.label), Middle (centerHero.label), and Right (rightSlot.label) labels MUST all use the EXACT SAME font family (labelTypography.font), the EXACT SAME font weight (labelTypography.fontWeight), the EXACT SAME pixel font size (unifiedLabelPx, computed strictly from labelTypography.fontSizeSp without multiplying centerHero.label by 1.5f), and the EXACT SAME high-contrast text color (defaultTextColor: pure black in Day Mode, pure white in Night Mode).
  - All three labels must be drawn via drawFixedLabel (no auto-scaling while loop) anchored to the exact same top line (labelTopY = height * 0.06f): Left Paint.Align.LEFT, Middle Paint.Align.CENTER, Right Paint.Align.RIGHT.

## 4. NEON GLASS ZONE PALETTE & 3-PASS GLOSS SHADER (ZonePalette.NEON_GLASS)
- ZonePalette.NEON_GLASS ("Neon Glass") 7-Zone Color Definitions:
  - Z1 (Frosted Ice Blue): top #94A3B8, core #38BDF8, rim #7DD3FC
  - Z2 (Electric Cyan): top #00F0FF, core #0284C7, rim #38BDF8
  - Z3 (Neon Emerald): top #00FF87, core #059669, rim #34D399
  - Z4 (Laser Amber Gold): top #FFE600, core #D97706, rim #FDE047
  - Z5 (Solar Blaze Orange): top #FF6B00, core #EA580C, rim #FB923C
  - Z6 (Hot Neon Crimson): top #FF0055, core #E11D48, rim #FB7185
  - Z7 (Ultraviolet Purple): top #D946EF, core #9333EA, rim #E879F9
- 3-Pass Glossy Glass Rendering (drawZoneSurface):
  - Whenever globalZonePalette == ZonePalette.NEON_GLASS, render zone pills, workout graph bars, target corridor boxes, and zone-filled cards using a 3-pass glossy shader:
    1. Vertical LinearGradient from the zone's top highlight color to its deep core color.
    2. Specular top glass reflection across the top 44% of the shape using a vertical LinearGradient from #70FFFFFF to #00FFFFFF.
    3. Luminous neon rim border (1.5f strokeWidth in the zone's rim color, or 3.0f bright rim on the active zone pill).

## 5. KAROO OS WORKOUT IPC MAPPING, CACHING & LIFECYCLE RULES
- Inverted Karoo OS Duration Stream Identifiers (Keep Locked in KustomFieldCatalog.kt):
  - "workout-duration-remaining" (WRK REM) maps to DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION.
  - "workout-interval-time-remaining" (INT REM) maps to DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION.
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
