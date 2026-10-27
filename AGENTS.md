# Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- Key architecture layers:
  - `data/model/`: `KustomSettings`, `KustomFieldCatalog`, `KustomFieldConfig`, `CustomComboInstance` (5 customizable 3-in-1 blocks), `GlobalTypographyConfig`, `GlobalColorConfig`, `ZonePalette`, `SmoothingOption`, `ZoneFormat`, `KustomFont`.
  - `data/repository/`: `KarooSystemRepository` and `KustomSettingsRepository` (Preferences DataStore `"kustom_settings"`).
  - `domain/`: `KustomFieldRenderer` (single fields + 3-in-1 combo blocks), `KustomColorAndSmoothing`, `KustomTypefaceResolver`.
  - `extension/`: `KustomExtension` and `types/KustomDataType`.
  - `ui/`: `MainActivity` and `KustomSettingsViewModel`.
- **CRITICAL `app/build.gradle.kts` SDK Levels**:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Keep `minSdk = 26` and `targetSdk = 34` (with `compileSdk = 35` or `release(37)`).

## 2. Strict Karoo Page Setup (`extension_info.xml` & `ic_kustom_field.xml`) Rules
- Every field in `KustomFieldCatalog.allFields` (including standard telemetry fields and the 5 customizable 3-In-One Blocks) MUST have a matching `<DataType>` entry in `app/src/main/res/xml/extension_info.xml` and `@string/...` entry in `app/src/main/res/values/strings.xml`.
- In `res/xml/extension_info.xml`, use bare attributes ONLY (`id="kustom"`, `typeId`, `displayName="@string/..."`, `description="@string/..."`, `graphical="true"`, `icon="@drawable/ic_kustom_field"`). Never use `android:` prefixes.

## 3. True 1:1 Font Scaling (NEVER Stretch Fonts) & Field Formatting
- **NEVER Stretch Fonts**:
  - NEVER set `paint.textScaleX` to anything other than `1.0f` in `KustomFieldRenderer` or `KustomTypefaceResolver`.
  - NEVER use `ContentScale.FillBounds` on preview `Image` composables in `MainActivity`. Always pass the exact pixel `width` and `height` of the Compose preview container (`BoxWithConstraints`) into `KustomFieldRenderer.render(...)` and render the `Image` with `contentScale = ContentScale.Fit` so fonts retain their true 1:1 aspect ratio.
- **Numbers Only & Dynamic Labels**:
  - Format numeric values without unit suffixes, and dynamically reflect active smoothing (`"3S POWER"` vs `"POWER"`) and sensor status typography consistently across standard and 3-in-1 combo fields.

## 4. State Isolation Rules (Prevent Slider & Pill Cross-Talk)
- **ALWAYS Use `rememberUpdatedState` Inside Custom Gesture Controls**:
  - Inside `KarooSteppedSlider` and segmented pill controls in `MainActivity`, ALWAYS wrap callbacks with `val currentCallback by rememberUpdatedState(callback)` before referencing them inside `Modifier.pointerInput(...)` so gesture handlers never capture a stale state snapshot from an earlier composition.
- **ALWAYS Use Atomic Per-Property Updates in `KustomSettingsRepository` / `KustomSettingsViewModel`**:
  - Mutate individual settings or read-modify-write inside `dataStore.edit { ... }` rather than passing stale UI-captured `.copy(...)` objects that could overwrite sibling properties.

## 5. Architectural Hygiene, Dead-Code & `karoo-ext` Lifecycle Rules
- **Single Source of Truth for Models**: Do not keep duplicate or superseded model classes, enums, or unused helper files in `data/model/`, `domain/`, or `ui/`.
- **Strict `karoo-ext` Coroutine & Consumer Cleanup**:
  - In `KustomDataType.startStream(emitter)` and `startView(config, emitter)`, every launched coroutine `Job` and every `karooSystem.addConsumer` listener (including multi-stream `combine` flows for 3-in-1 combo blocks) MUST be cancelled/removed inside `emitter.setCancellable { job.cancel() }` so switching pages or ending a ride never leaks coroutines or binder callbacks.
  - In `startView(...)`, ALWAYS emit `emitter.onNext(UpdateGraphicConfig(showHeader = false))` before pushing `RemoteViews` updates so Karoo OS hides its default system field header.
- **Process-Wide Singletons & Caching**:
  - `Preferences DataStore` and `KarooSystemRepository` (`KarooSystemService`) must be singletons scoped to `applicationContext`.
  - `KustomTypefaceResolver` must cache loaded `Typeface` instances in memory so fonts are never re-parsed from `assets/fonts/` on every 1 Hz frame.
- **Review-First Refactoring Protocol**: When asked for an architectural review or cleanup, operate in strict READ-ONLY mode first—present findings sorted by risk (`Low`, `Medium`, `High`) and wait for explicit user approval before modifying or deleting any files.
