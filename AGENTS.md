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

## 2. KAROO OS WORKOUT IPC MAPPING & PRE-RIDE vs. IN-RIDE ISOLATION
- Inverted Karoo OS Duration Stream Identifiers (Keep Locked in KustomFieldCatalog.kt):
  - In Karoo OS's internal IPC broadcast, the two duration DataType.Type constants are inverted:
    * "workout-duration-remaining" (WRK REM) MUST map to DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION (broadcasts total remaining workout seconds, e.g. 7200s = 02:00:00).
    * "workout-interval-time-remaining" (INT REM) MUST map to DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION (broadcasts remaining interval step seconds, e.g. 600s = 10:00).
- Pre-Ride (!isInRide) vs. In-Ride (isInRide == true) Workout Resolution:
  - Because workout .fit/.zwo files always sit on the SD card, hasDiskWorkout (activeWorkout?.steps?.isNotEmpty() == true) is true even when the user goes on a regular free ride without a workout!
  - Therefore, disk-based pre-ride fallbacks (preRideWorkoutDuration and firstStepTargetStr) MUST ONLY be used when !isInRide (pre-ride before pressing Start, or when isMock == true):
    * val hasLiveWorkoutStream = liveWorkoutTotalSec != null || liveWorkoutIntSec != null || liveWorkoutTargetStr != null || liveWorkoutStepInfo != null
    * val canUsePreRideDiskWorkout = !isInRide && hasDiskWorkout
    * val isWorkoutActiveOnKaroo = isMock || hasLiveWorkoutStream || canUsePreRideDiskWorkout
  - Once a ride has started (isInRide == true, meaning RideState.Recording or RideState.Paused), Kustom MUST ONLY use live Karoo workout stream values (liveWorkoutTotalSec, liveWorkoutIntSec, liveWorkoutTargetStr) to decide if a workout is active. If isInRide == true and hasLiveWorkoutStream == false (a regular free ride without a workout), all workout fields MUST display "No Workout" (and 3-in-1 combo blocks with Center = Workout Target MUST display "3S POWER" / "POWER", never firstStepTargetStr from disk!).

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
  - Reference Digit Height ("88") for Vertical Measurement in drawTextInBox: Always measure vertical height (refBounds.height()) and vertical baseline (refBounds.top / refBounds.exactCenterY()) using paint.getTextBounds("88", 0, 2, refBounds) while measuring renderText for horizontal width (widthBounds.width()). When renderText == "--", use Paint.Style.FILL and strokeWidth = 0f.

## 4. Workout Graph Field & FIT/ZWO Storage Rules
- Mandatory In-Memory Caching in WorkoutFileParser:
  - Cache parsed workouts in memory and throttle directory scans to at most once every 10 seconds, invalidating the cache immediately whenever the live Karoo workout fingerprint (expectedTotalDurationSec or expectedStepCount) changes!
  - Always wrap FileInputStream in BufferedInputStream(..., 8192).use { ... }.
- Permissions: Keep <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" /> in AndroidManifest.xml.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
