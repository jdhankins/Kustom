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

## 2. KAROO OS WORKOUT IPC MAPPING, STREAM RESET & PRE-RIDE REFRESH
- Inverted Karoo OS Duration Stream Identifiers (Keep Locked in KustomFieldCatalog.kt):
  - "workout-duration-remaining" (WRK REM) MUST map to DataType.Type.WORKOUT_REMAINING_INTERVAL_DURATION (broadcasts total remaining workout seconds).
  - "workout-interval-time-remaining" (INT REM) MUST map to DataType.Type.WORKOUT_REMAINING_TOTAL_DURATION (broadcasts remaining interval step seconds).
- Reset Stream Consumers on RideState Transitions & Pre-Ride Refresh:
  - When a ride is stopped/deleted (RideState transitions from Recording/Paused to Idle), Kotlin combine(...) flows will hold stale DataPoints from the ended ride unless the stream subscriptions are recreated!
  - Always include a ride-session / pre-ride refresh trigger in flatMapLatest so that:
    1. Transitioning between RideState.Idle, RideState.Recording, and RideState.Paused immediately resets/re-subscribes the workout stream consumers, wiping out any stale workout DataPoints from a previous ride.
    2. While in RideState.Idle (pre-ride screen), a periodic 3-second refresh re-queries the workout stream consumers so loading or unloading a workout in the drawer refreshes the ride page without waiting for Start to be pressed.
  - Do NOT use hasDiskWorkout (files on /sdcard/) to force isWorkoutActiveOnKaroo = true on non-workout rides, because .fit/.zwo files remain in /sdcard/FitFiles/ even after a ride is deleted or a workout is unloaded! Only use WorkoutFileParser for the Workout Graph when live Karoo workout streams confirm a workout is loaded.

## 3. REAL-TIME SDK DEBUG FIELDS (debug-workout, debug-power, debug-hr, debug-sensors)
- Exact Catalog typeIds Must Match KustomDataType.kt:
  - Always match the exact typeIds registered in KustomFieldCatalog.kt: "debug-workout", "debug-power", "debug-hr", and "debug-sensors".
  - Never use streamDataFlow(DataType.Type.RIDE_STATE) because RideState is a KarooEvent (use karooSystemRepository.rideStateFlow).
- Dual Output (Logcat + 2-Column On-Screen Grid):
  - Logcat Output (tag = "KUSTOM_SDK_DUMP"): Every 2 seconds, log the complete, unabbreviated StreamState (Idle, Searching, NotAvailable, Streaming) and raw DataPoint.values map for every subscribed stream in that debug category so the developer can copy/paste raw SDK values directly from Android Studio Logcat.
  - On-Screen Output (renderDebugCategoryField): Render a 2-column monospace grid (Column 1 at x = 3%..48%, Column 2 at x = 52%..97%) with abbreviated key names (stripping "TYPE_", "FIELD_", "WORKOUT_", "_ID") and 1 key:value per row so up to 16-20 datapoints fit cleanly on-screen without ever clipping off the right edge.

## 4. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- LABELS NEVER AUTO-SCALE; ONLY VALUES AUTO-SCALE:
  - Labels (leftSlot.label, centerHero.label, rightSlot.label) MUST NEVER be passed through a shrinking while loop. All labels across every card must be drawn via drawFixedLabel at the exact same fixed pixel size fixedLabelPx and top-anchored to labelTopY = height * 0.07f.
  - Only Values (leftSlot.value, rightSlot.value, centerHero.value) auto-scale via drawTextInBox to fit their bounding box.
  - Reference Digit Height ("88") for Vertical Measurement in drawTextInBox: Always measure vertical height (refBounds.height()) and vertical baseline using paint.getTextBounds("88", 0, 2, refBounds) while measuring renderText for horizontal width (widthBounds.width()). When renderText == "--", use Paint.Style.FILL and strokeWidth = 0f.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
