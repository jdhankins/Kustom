# Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- Key architecture layers:
  - `data/model/`: `KustomSettings`, `KustomFieldCatalog`, `KustomFieldConfig`, `CustomComboInstance` (5 static 3-in-1 blocks: Block 1 Power, Block 2 HR, Block 3 Grade, Block 4 Workout, Block 5 Cadence), `GlobalTypographyConfig`, `GlobalColorConfig`, `ZonePalette`, `SmoothingOption`, `ZoneFormat`, `KustomFont`.
  - `data/repository/`: `KarooSystemRepository` and `KustomSettingsRepository` (Preferences DataStore `"kustom_settings"`).
  - `domain/`: `KustomFieldRenderer` (single fields, 3-in-1 combo blocks, and workout graph HUD), `WorkoutFileParser` (local `.fit` and `.zwo` parser), `KustomColorAndSmoothing`, `KustomTypefaceResolver`.
  - `extension/`: `KustomExtension` and `types/KustomDataType`.
  - `ui/`: `MainActivity` and `KustomSettingsViewModel`.
- **CRITICAL `app/build.gradle.kts` SDK Levels**:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Keep `minSdk = 26` and `targetSdk = 34` (with `compileSdk = 35` or `release(37)`).

## 2. 3-In-One Configurable Blocks & Workout Target Center Alignment Rules
- **Static 3-In-One Blocks**: `combo-block-1` through `combo-block-5` are pre-registered in `res/xml/extension_info.xml` and `KustomFieldCatalog`.
- **Center-Only Workout Target Logic**:
  - Workout target metrics (`"workout-target-power"`, `"workout-primary-target"`) MUST ONLY be selectable in the **Center** position of a 3-in-1 block (never in Left or Right corner dropdowns).
  - When a workout target metric is selected in the Center position of a 3-in-1 block:
    1. **Top Center Label (`comboCenterLabel`)**: Dynamically renders the active workout target range or wattage extracted from `DataType.Type.WORKOUT_POWER_TARGET` / `WORKOUT_PRIMARY_TARGET` (e.g., `"117-146W"`, falling back to `"117-146"` in preview/mock mode).
    2. **Primary Center Value (`primaryValText`)**: Streams live Power telemetry (`KustomFieldCatalog.POWER.resolveSdkDataTypeId(smoothing)`), displaying live watts when paired or `"No Sensor"` in red when unpaired.
- **Explicit `DataType.Field` Extraction (No Blanket Loops)**:
  - Never use `val targetDp = priDp ?: pDp` (which selects a non-null `0.0` `WORKOUT_PRIMARY_TARGET` `DataPoint` over a valid `WORKOUT_POWER_TARGET` `DataPoint`). Always evaluate `extractTargetPowerString(pDp) ?: extractTargetPowerString(priDp)` using explicit `DataType.Field` keys (`WORKOUT_TARGET_MIN_VALUE`, `WORKOUT_TARGET_MAX_VALUE`, `WORKOUT_TARGET_VALUE`, `WORKOUT_TARGET_OUTPUT_VALUE`, `SINGLE`).

## 3. Workout Graph Field & FIT/ZWO Storage Rules
- **Mandatory In-Memory Caching in `WorkoutFileParser`**:
  - NEVER re-scan directories or re-parse `.fit`/`.zwo` files on every 1 Hz frame. Cache the parsed `ParsedWorkout` keyed by `(filePath, lastModified, ftp)` and throttle directory timestamp checks to at most once every 10 seconds.
  - Always wrap `FileInputStream` in `BufferedInputStream(..., 8192).use { ... }`.
- **Multi-Trace Stream Subscription**:
  - For `"workout-graph"`, `KustomDataType` must subscribe concurrently to `POWER`, `HEART_RATE`, and `CADENCE` streams (plus workout elapsed/remaining duration) and pass immutable list snapshots (`.toList()`) to `KustomFieldRenderer.renderWorkoutGraph`.

## 4. True 1:1 Font Scaling, State Isolation & `karoo-ext` Lifecycle Rules
- **NEVER Stretch Fonts**: Keep `paint.textScaleX = 1.0f` and use `ContentScale.Fit` with exact container pixel dimensions on all preview `Image` composables.
- **State Isolation**: Always use `rememberUpdatedState` inside custom gesture controls (`KarooSteppedSlider`) and atomic DataStore updates in `KustomSettingsRepository`.
- **Strict `karoo-ext` Cleanup**: Always emit `UpdateGraphicConfig(showHeader = false)` at the start of `startView`, and cancel all coroutines/consumers inside `emitter.setCancellable { job.cancel() }`.
