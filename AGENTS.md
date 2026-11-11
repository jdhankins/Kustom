# Project Rules: Kustom (com.hank.kustom) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: com.hank.kustom. Extension ID: "kustom".
- Primary extension service: com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1").
- Key architecture layers:
  - data/model/: KustomSettings, KustomFieldCatalog, KustomFieldConfig, CustomComboInstance (5 static 3-in-1 blocks), GlobalTypographyConfig, GlobalColorConfig, ZonePalette (including NEON_GLASS "Neon Glass"), SmoothingOption, ZoneFormat, KustomFont.
  - data/repository/: KarooSystemRepository and KustomSettingsRepository (Preferences DataStore "kustom_settings").
  - domain/: FullWidthLayoutEngine (centralized full-width slot layout engine), KustomFieldRenderer (single fields, 3-in-1 combo blocks, experimental target power corridor & workout graph HUD, and 2-column debug terminal), WorkoutFileParser (local .fit and .zwo parser), KustomColorAndSmoothing, KustomTypefaceResolver.
  - extension/: KustomExtension and types/KustomDataType.
  - ui/: MainActivity and KustomSettingsViewModel.
- CRITICAL app/build.gradle.kts SDK Levels:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Keep minSdk = 26 and targetSdk = 34 (with compileSdk = 35 or release(37)).

## 2. TELEMETRY EXTRACTION, NEGATIVE VALUES, COASTING CADENCE & ZONE BOUNDARIES
- Allow Negative Telemetry Values (Grade & Temperature in KustomDataType.kt):
  - NEVER filter non-workout telemetry values with `dynamicValue >= 0.0`! Road grade ("grade") on descents and ambient temperature ("temperature") in winter are negative numbers. Any `dynamicValue != null && !dynamicValue.isNaN()` MUST be formatted via `definition.formatNumericValueOnly(dynamicValue, isImperial)` (never clamped to 0.0).
  - In FullWidthLayoutEngine.kt and KustomFieldRenderer.kt, check `!renderText.drop(1).contains("-")` for superscript eligibility so negative decimals like `"-5.4"` render as `-5⁴`.
- Coasting Cadence (0 RPM) Must Display "0", Not "No Sensor" (KustomDataType.kt):
  - When a rider coasts downhill without pedaling, crank cadence sensors stop sending revolution events and Karoo OS transitions CADENCE to `StreamState.Idle` (or temporarily `NotAvailable` / `null` value).
  - In KustomDataType.kt, `StreamState.Idle` MUST render `definition.formatNumericValueOnly(0.0, isImperial)` ("0"), NEVER "No Sensor"! Additionally, once a cadence/power/torque sensor has emitted at least one valid sample during the ride (`hasReceivedSample == true`), any temporary coasting dropout during an active ride (`isInRide == true`) must render `"0"` rather than flashing `"No Sensor"`.
- Contiguous Integer Zone Matching (Zero Boundary Gaps in KustomColorAndSmoothing.kt):
  - Karoo's `UserProfile.Zone(min: Int, max: Int)` has integer bounds (e.g., Z1 = 0..164, Z2 = 165..210). Never check raw unrounded floating-point `watts in minD..maxD` (where `164.6W` falls between `164.0` and `165.0`, returning `idx = -1` and flashing `lastIndex` RED!).
  - Always round `watts` (`val w = kotlin.math.round(watts)`) and `bpm` (`val b = kotlin.math.round(bpm)`) first, and match using contiguous upper bounds: `powerZones.indexOfFirst { (_, max) -> if (max <= 5) pctFtp <= max.toDouble() else w <= max.toDouble() }`.
- VAM Unit Conversion (m/s to m/h in KustomFieldCatalog.kt):
  - Karoo OS streams `VERTICAL_SPEED`, `AVERAGE_VERTICAL_SPEED_30S` ("vam"), and `AVERAGE_VERTICAL_SPEED` ("avg-vam") in meters per second (m/s). Multiply `rawValue` by `3600.0` to convert to vertical meters per hour (m/h).

## 3. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- Horizontal & Vertical Column Allocation:
  - Left Column: 3.0% to 30.5% (27.5% width), strictly left-aligned (Paint.Align.LEFT).
  - Center Hero Column: 32.0% to 68.0% (36.0% width when side slots are present; 8.0% to 92.0% when both side slots are Empty).
  - Right Column: 69.5% to 97.0% (27.5% width), right-aligned (Paint.Align.RIGHT) when plain text.
  - Corner Value Vertical Box: valueBoxTop = height * 0.26f, and valueBoxBottom = if (centerHero.hasBottomBar) height * 0.72f else height * 0.82f.
- UNIFIED HEADER LABEL TYPOGRAPHY & OUTDOOR VISIBILITY (Left, Middle, and Right Labels):
  - Left (leftSlot.label), Middle (centerHero.label), and Right (rightSlot.label) labels MUST all use the EXACT SAME font family (labelTypography.font), the EXACT SAME font weight (labelTypography.fontWeight), the EXACT SAME pixel font size (unifiedLabelPx), and the EXACT SAME high-contrast text color (defaultTextColor: pure black in Day Mode, pure white in Night Mode).

## 4. NEON GLASS ZONE PALETTE & 3-PASS GLOSS SHADER (ZonePalette.NEON_GLASS)
- MUST Apply 3-Pass Glossy Glass Shader (`drawNeonGlassRoundRect`) to ALL Zone Surfaces (7-zone bar pills, workout graph bars, target corridor, corner pills, and full-card backgrounds).

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.

