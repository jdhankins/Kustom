Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- **CRITICAL `app/build.gradle.kts` SDK Levels**:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Always set `compileSdk = 35`, `minSdk = 26`, and `targetSdk = 34` in `app/build.gradle.kts`.

## 2. Strict Karoo Page Setup (`extension_info.xml` & `ic_kustom_field.xml`) Rules
- Every field in `KustomFieldCatalog.allFields` MUST have a matching `<DataType>` entry in `app/src/main/res/xml/extension_info.xml` and `@string/...` entry in `app/src/main/res/values/strings.xml`.
- In `res/xml/extension_info.xml`, use bare attributes ONLY (`id="kustom"`, `typeId`, `displayName="@string/..."`, `description="@string/..."`, `graphical="true"`, `icon="@drawable/ic_kustom_field"`). Never use `android:` prefixes.
- In `res/drawable/ic_kustom_field.xml`, use a clean `24dp x 24dp` monochrome vector icon with a transparent background.
- Title every `@string/field_*_name` in `strings.xml` using `"Parent - Child"` format (e.g., `"Power - Power"`, `"Climb - Grade"`, `"Time - Ride Time"`).

## 3. True 1:1 Font Scaling (NEVER Stretch Fonts) & Field Formatting
- **NEVER Stretch Fonts**:
  - NEVER set `paint.textScaleX` to anything other than `1.0f`.
  - NEVER use `ContentScale.FillBounds` on preview `Image` composables. Always pass the exact pixel `width` and `height` of the Compose preview container (`BoxWithConstraints` `constraints.maxWidth` and `constraints.maxHeight`) into `KustomFieldRenderer.render(...)` and render the `Image` with `contentScale = ContentScale.Fit` so fonts retain their true 1:1 aspect ratio.
  - Scale `labelPaint.textSize` and `valuePaint.textSize` proportionally to the container height (`height * (fontSizeSp / 68f)` for label, `height * (fontSizeSp / 86f)` for value), and only step down `textSize` if `paint.measureText(text) > width * 0.92f`.
- **Numbers Only (No Units)**: NEVER append units (`"W"`, `"km/h"`, `"mph"`, `"bpm"`, `"rpm"`, `"%"`, `"m"`) to numeric values.
- **Dynamic Smoothing Label**: When smoothing (`3s`, `5s`, `10s`, `30s`) is active on a field that supports smoothing, prefix its rendered header label with the smoothing window (e.g., `"3S POWER"`).

## 4. State Isolation Rules (Prevent Slider & Pill Cross-Talk)
- **ALWAYS Use `rememberUpdatedState` Inside Custom Gesture Controls**:
  - Inside `KarooSteppedSlider` and `KarooSegmentedPill`, ALWAYS wrap callbacks with `val currentCallback by rememberUpdatedState(callback)` before referencing them inside `Modifier.pointerInput(...)` so gesture handlers never capture a stale state snapshot from an earlier composition.
- **ALWAYS Use Atomic Per-Property Updates in `KustomSettingsRepository`**:
  - Expose atomic update functions (`updateFontFamily`, `updateValueFontSizeSp`, `updateValueFontWeight`, `updateLabelFontSizeSp`, `updateLabelFontWeight`, `updateFieldSmoothing`, `updateFieldColorMode`) that read and mutate only that single setting inside `dataStore.edit { ... }`. Never pass `.copy(...)` of a UI-captured config object that could overwrite sibling properties.