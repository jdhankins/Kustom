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

## 2. OFFICIAL karoo-ext WORKOUT STREAM KEYS, DETECTION & FILE MATCHING
- Exact Official io.hammerhead.karooext.models.DataType.Field Constants:
  - NEVER invent field key strings like "FIELD_WORKOUT_REMAINING_TOTAL_DURATION_ID" or "FIELD_WORKOUT_REMAINING_INTERVAL_DURATION_ID" (they do not exist in karoo-ext!).
  - Always use the official DataType.Field constants from karoo-ext:
    * DataType.Field.WORKOUT_REMAINING_TIME ("FIELD_WORKOUT_REMAINING_TIME_ID") -> Total workout remaining time (WRK REM)
    * DataType.Field.WORKOUT_TIME_TO_STEP_FINISH ("FIELD_WORKOUT_TIME_TO_STEP_FINISH_ID") -> Current interval step remaining time (INT REM). Note: NEVER filter out keys containing "STEP" when looking for interval time remaining, because "FIELD_WORKOUT_TIME_TO_STEP_FINISH_ID" contains the substring "STEP"!
    * DataType.Field.WORKOUT_ELAPSED_TIME ("FIELD_WORKOUT_ELAPSED_TIME_ID") -> Workout elapsed time
    * DataType.Field.WORKOUT_CURRENT_STEP ("FIELD_WORKOUT_CURRENT_STEP_ID") -> Current step index
    * DataType.Field.WORKOUT_STEP_COUNT ("FIELD_WORKOUT_STEP_COUNT_ID") -> Total steps in workout
    * DataType.Field.WORKOUT_STATE ("FIELD_WORKOUT_STATE_ID") -> Workout state
    * DataType.Field.WORKOUT_TARGET_VALUE ("FIELD_WORKOUT_TARGET_VALUE_ID"), WORKOUT_TARGET_MIN_VALUE ("FIELD_WORKOUT_TARGET_MIN_VALUE_ID"), WORKOUT_TARGET_MAX_VALUE ("FIELD_WORKOUT_TARGET_MAX_VALUE_ID") -> Target power/HR/cadence values
- Single Source of Truth for Whether a Workout Is Active/Loaded (isWorkoutActiveOnKaroo):
  - When isMock == false (config.preview == false), a workout is loaded/active on the Karoo IF AND ONLY IF at least one of the live Karoo workout streams (streamState for a workout type, targetPowerState, or primaryTargetState) is StreamState.Streaming and contains a positive (> 0.0) value for WORKOUT_REMAINING_TIME, WORKOUT_TIME_TO_STEP_FINISH, WORKOUT_STEP_COUNT, WORKOUT_TARGET_MIN_VALUE, WORKOUT_TARGET_MAX_VALUE, or WORKOUT_TARGET_VALUE.
  - NEVER use WorkoutFileParser.resolveActiveWorkoutFile(...) to decide whether a workout is loaded, and NEVER fall back to preRideWorkoutDuration from disk files when the Karoo workout streams are not streaming a loaded workout!
- Fingerprint File Matching in WorkoutFileParser.resolveActiveWorkoutFile:
  - Selecting a workout in Karoo OS does NOT update file.lastModified() on disk! Never simply pick sortedByDescending { it.lastModified() }.firstOrNull().
  - Instead, pass the live workout telemetry fingerprint from Karoo's streams (expectedTotalDurationSec = remainingTimeSec + elapsedTimeSec, expectedStepCount, expectedCurrentStepDurationSec) into resolveActiveWorkoutFile and select the .fit/.zwo candidate file whose parsed totalDurationSeconds, steps.size, and step duration match the live Karoo workout!

## 3. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
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
