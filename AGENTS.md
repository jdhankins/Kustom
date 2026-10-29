# Project Rules: Kustom (com.hank.kustom) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: com.hank.kustom. Extension ID: "kustom".
- Primary extension service: com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1").
- Key architecture layers:
    - data/model/: KustomSettings, KustomFieldCatalog, KustomFieldConfig, CustomComboInstance (5 static 3-in-1 blocks: Block 1 Power, Block 2 HR, Block 3 Grade, Block 4 Workout, Block 5 Cadence), GlobalTypographyConfig, GlobalColorConfig, ZonePalette, SmoothingOption, ZoneFormat, KustomFont.
    - data/repository/: KarooSystemRepository and KustomSettingsRepository (Preferences DataStore "kustom_settings").
    - domain/: FullWidthLayoutEngine (centralized full-width slot layout engine), KustomFieldRenderer (single fields, 3-in-1 combo blocks, target power corridor, and workout graph HUD), WorkoutFileParser (local .fit and .zwo parser), KustomColorAndSmoothing, KustomTypefaceResolver.
    - extension/: KustomExtension and types/KustomDataType.
    - ui/: MainActivity and KustomSettingsViewModel.
- CRITICAL app/build.gradle.kts SDK Levels:
    - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
    - Keep minSdk = 26 and targetSdk = 34 (with compileSdk = 35 or release(37)).

## 2. STRICT LIVE-RIDE vs. MOCK-PREVIEW ISOLATION (Zero Leaked Mock Data in Rides)
- How isMockMode Must Be Defined in KustomDataType.kt:
    - Set val isMockMode = config.preview (NEVER !isInRide || config.preview). Mock numbers are ONLY allowed in the in-app configuration screens (MainActivity) and Karoo's Page Editor preview (config.preview == true). On the actual ride screen (config.preview == false), isMockMode MUST ALWAYS be false (both pre-ride and in-ride).
- NEVER Leak Hardcoded or Disk-Parsed Target Ranges When isMock == false:
    - In KustomFieldCatalog.kt, resolveDynamicLabel MUST NEVER return "117-146". It must return "TARGET POWER" or "TARGET".
    - In KustomDataType.kt, NEVER use extractTargetPowerString(targetDataPoint) ?: "117-146" unguarded. When isMock == false and extractTargetPowerString(targetDataPoint) == null (regular ride without a workout), comboCenterLabel on a 3-in-1 block MUST fall back to KustomFieldCatalog.POWER.resolveDynamicLabel(resolvedConfig.smoothingOption) (e.g. "3S POWER" or "POWER").
    - In KustomDataType.kt inside the workout-target-corridor branch, NEVER call WorkoutFileParser.resolveActiveWorkoutFile(...). A file sitting on the SD card does not mean a workout is active! When isMock == false, workout-target-corridor must ONLY use live values from targetDataPoint; if no workout is active during the ride, minTarget, maxTarget, and targetWatts must be null, displaying "--" under "TARGET" and hiding the green target zone on the bottom bar.
    - In KustomFieldRenderer.kt, NEVER use powerValueWatts ?: 214.0 when mappedColor is null.

## 3. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- Strict Non-Overlapping Horizontal Columns (Sum = 100%):
    - Left Column: 3.5% to 28.5% (25% width), strictly left-aligned (Paint.Align.LEFT).
    - Left Gutter: 28.5% to 30.5% (2.0% empty buffer).
    - Center Hero Column: 30.5% to 69.5% (39% width when side slots are present; 8.0% to 92.0% when both side slots are Empty).
    - Right Gutter: 69.5% to 71.5% (2.0% empty buffer).
    - Right Column: 71.5% to 96.5% (25% width), right-aligned (Paint.Align.RIGHT) when plain text, or centered with 11% inner horizontal padding on each side (maxTextW = pillWidth * 0.78f) when isStatusBadge is true.
- LABELS NEVER AUTO-SCALE; ONLY VALUES AUTO-SCALE:
    - Labels (leftSlot.label, centerHero.label, rightSlot.label) MUST NEVER be passed through a shrinking while loop. All labels across every card must be drawn via drawFixedLabel at the exact same fixed pixel size fixedLabelPx and top-anchored to labelTopY = height * 0.07f.
    - Only Values (leftSlot.value, rightSlot.value, centerHero.value) auto-scale via drawTextInBox to fit their bounding box.
    - Reference Digit Height ("88") for Vertical Measurement in drawTextInBox: Always measure vertical height (refBounds.height()) and vertical baseline (refBounds.top / refBounds.exactCenterY()) using paint.getTextBounds("88", 0, 2, refBounds) while measuring renderText for horizontal width (widthBounds.width()). When renderText == "--", use Paint.Style.FILL and strokeWidth = 0f so dashes never bloat into thick black bricks.

## 4. Workout Graph Field & FIT/ZWO Storage Rules
- Mandatory In-Memory Caching in WorkoutFileParser:
    - NEVER re-scan directories or re-parse .fit/.zwo files on every 1 Hz frame. Cache the parsed ParsedWorkout keyed by (filePath, lastModified, ftp) and throttle directory timestamp checks to at most once every 10 seconds.
    - Always wrap FileInputStream in BufferedInputStream(..., 8192).use { ... }.
- Permissions: Keep <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" /> in AndroidManifest.xml.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.

