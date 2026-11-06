# Project Rules: Kustom (com.hank.kustom) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: com.hank.kustom. Extension ID: "kustom".
- Primary extension service: com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1").
- Key architecture layers:
  - data/model/: KustomSettings, KustomFieldCatalog, KustomFieldConfig, CustomComboInstance (5 static 3-in-1 blocks: Block 1 Power, Block 2 HR, Block 3 Grade, Block 4 Workout, Block 5 Cadence), GlobalTypographyConfig, GlobalColorConfig, ZonePalette, SmoothingOption, ZoneFormat, KustomFont.
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
  - In KustomFieldCatalog.formatNumericValueOnly, "vam" and "avg-vam" MUST multiply rawValue (m/s) by 3600.0 to convert to vertical meters per hour (m/h): (rawValue * 3600.0).roundToInt().coerceAtLeast(0).toString(). Never call rawValue.toInt() directly on m/s vertical speed (which truncates values < 1.0 m/s to 0)!
- Negative Number Superscript Eligibility (FullWidthLayoutEngine.kt & KustomFieldRenderer.kt):
  - When checking if a decimal or time string is eligible for superscript formatting (e.g., "-3.4" -> "-3" with superscript "4"), check !renderText.drop(1).contains("-") instead of !renderText.contains("-"). A leading minus sign at index 0 (negative grade or temperature) MUST use superscript formatting, while range strings with an interior hyphen ("115-160") are excluded.

## 3. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- UNIFIED HEADER LABEL TYPOGRAPHY & OUTDOOR VISIBILITY (Left, Middle, and Right Labels):
  - Left (leftSlot.label), Middle (centerHero.label), and Right (rightSlot.label) labels MUST all use the EXACT SAME font family (labelTypography.font), the EXACT SAME font weight (labelTypography.fontWeight — never hardcode 700), the EXACT SAME pixel font size (unifiedLabelPx, computed strictly from labelTypography.fontSizeSp without multiplying centerHero.label by 1.5f), and the EXACT SAME high-contrast text color (defaultTextColor: pure black in Day Mode, pure white in Night Mode — never washed-out grey mutedTextColor).
  - All three labels must be drawn via drawFixedLabel (no auto-scaling while loop) anchored to the exact same top line (labelTopY = height * 0.06f):
    * Left Label: Paint.Align.LEFT at leftColStart
    * Middle Label: Paint.Align.CENTER at width / 2f
    * Right Label: Paint.Align.RIGHT at rightColEnd
- Value Font Size & Weight Logic:
  - Keep the value auto-scaling (drawTextInBox) and superscript formatting for leftSlot.value, centerHero.value, and rightSlot.value unchanged.

## 4. KAROO OS WORKOUT IPC MAPPING & STREAM RESET
- Inverted Karoo OS Duration Stream Identifiers (Keep Locked in KustomFieldCatalog.kt):
  - "workout-duration-remaining" (WRK REM) maps to DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION.
  - "workout-interval-time-remaining" (INT REM) maps to DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
