# Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- **CRITICAL `app/build.gradle.kts` SDK Levels**:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Keep `minSdk = 26` and `targetSdk = 34` (with `compileSdk = 35` or `release(37)`).

## 2. Workout Target & Corridor Fields
- **Target Power Corridor (`workout-target-corridor`)**:
  - Renders a top status badge (`ON TARGET` in `#00E676`, `LOW -XW` in `#00B0FF`, `HIGH +XW` in `#FF5252`), a large centered hero power value, and a horizontal target corridor track.
  - The cursor needle MUST use 3-second smoothed power to ensure visual stability, while the target range is extracted from `DataType.Type.WORKOUT_POWER_TARGET` and `WORKOUT_PRIMARY_TARGET` (falling back to `WorkoutFileParser.resolveActiveWorkoutFile`).
- **3-In-One Blocks Center Target Alignment**:
  - When `"workout-target-power"` or `"workout-primary-target"` is selected in the Center position of a 3-in-1 block, the top center label (`comboCenterLabel`) renders the target range (e.g., `"117-146W"`), while the primary center value streams live power telemetry.

## 3. Workout Graph Field & FIT/ZWO Storage Rules
- **Mandatory In-Memory Caching in `WorkoutFileParser`**:
  - NEVER re-scan directories or re-parse `.fit`/`.zwo` files on every 1 Hz frame. Cache the parsed `ParsedWorkout` keyed by `(filePath, lastModified, ftp)` with a 10-second scan throttle.
  - Wrap `FileInputStream` in `BufferedInputStream(..., 8192).use { ... }`.
- **Permissions**: Keep `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />` in `AndroidManifest.xml` for Karoo 3 Scoped Storage compatibility.

## 4. True 1:1 Font Scaling, State Isolation & `karoo-ext` Lifecycle Rules
- **NEVER Stretch Fonts**: Keep `paint.textScaleX = 1.0f` and use `ContentScale.Fit` with exact container pixel dimensions on all preview `Image` composables.
- **State Isolation**: Always use `rememberUpdatedState` inside custom gesture controls (`KarooSteppedSlider`) and atomic DataStore updates in `KustomSettingsRepository`.
- **Strict `karoo-ext` Cleanup**: Always emit `UpdateGraphicConfig(showHeader = false)` at the start of `startView`, and cancel all coroutines/consumers inside `emitter.setCancellable { job.cancel() }`.
