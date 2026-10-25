# Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- **CRITICAL `app/build.gradle.kts` SDK Levels**:
  - Karoo 2 runs Android 8.1 (API 27) and Karoo 3 runs Android 12 (API 31/32).
  - Always set `compileSdk = 35`, `minSdk = 26`, and `targetSdk = 34` in `app/build.gradle.kts`. Do NOT use preview `compileSdk = release(37)` or `targetSdk = 37`, which generates AAPT2 binary XML chunks that Karoo OS fails to parse for `extension_info.xml`.

## 2. Strict Karoo Page Setup (`extension_info.xml` & `ic_kustom_field.xml`) Rules
- Every field in `KustomFieldCatalog.allFields` (covering both configurable and raw native `DataType.Type` metrics) MUST have a matching `<DataType>` entry in `app/src/main/res/xml/extension_info.xml` and `@string/...` entry in `app/src/main/res/values/strings.xml`.
- In `res/xml/extension_info.xml`, use bare attributes ONLY (`id="kustom"`, `typeId`, `displayName="@string/..."`, `description="@string/..."`, `graphical="true"`, `icon="@drawable/ic_kustom_field"`). Never use `android:` prefixes.
- In `res/drawable/ic_kustom_field.xml`, use a clean `24dp x 24dp` monochrome vector icon with a transparent background (no full-bleed `108x108` background rectangle) so Karoo OS tints the icon cleanly in `Select Data`.
- Title every `@string/field_*_name` in `strings.xml` using `"Parent - Child"` format (e.g., `"Power - Power"`, `"Climb - Grade"`, `"Time - Ride Time"`).

## 3. Field Value Formatting, Dynamic Labels & Auto-Fit Layout Constraints
- **Numbers Only (No Units)**: NEVER append units (`"W"`, `"km/h"`, `"mph"`, `"bpm"`, `"rpm"`, `"%"`, `"m"`) to numeric values. Render only the formatted number or time string (e.g., `"260"`, `"32.4"`, `"1:24:05"`).
- **Dynamic Smoothing Label**: When smoothing (`3s`, `5s`, `10s`, `30s`) is active on a field that supports smoothing, prefix its rendered header label with the smoothing window (e.g., `"3S POWER"`, `"5S POWER"`, `"10S SPEED"`; `"POWER"` when `0s`).
- **Auto-Fitting Large Font Scales (`KustomFieldRenderer.kt`)**:
  - Reserve the top `26%` of the bitmap for the label and the bottom `74%` for the main value.
  - Before drawing, iteratively step down `labelPaint.textSize` and `valuePaint.textSize` in a `while` loop until `paint.measureText(text) <= width * 0.90f` AND `(paint.fontMetrics.descent - paint.fontMetrics.ascent)` fits within its vertical band (`height * 0.22f` for label, `height * 0.66f` for value). This prevents any clipping or label-value collision when `56sp`, `64sp`, or `72sp` is selected.

## 4. Modernized Configuration Controls UI/UX (`MainActivity.kt`)
- **Top Navigation Bar**:
  - Strictly two tabs: **`Global`** and **`Fields`** (in that exact order). Remove `"Logs 📋"` and any other buttons from the top bar.
- **Preview Placement**:
  - **`Global` Tab**: Has a single preview card at the top, followed by `FONT FAMILY` and the 4 typography sliders inside a scrollable column with `bottom = 32.dp` padding.
  - **`Fields` Tab**: Has NO top-level global preview card. Instead, only `KustomFieldCatalog.configurableFields` (`supportsSmoothing || supportsZones`) are shown in the category accordion, and each child metric card displays its own `132.dp x 68.dp` live preview box alongside its title and description.
- **Modern Sleek Stepped Slider (`KarooSteppedSlider`)**:
  - Do NOT use the oversized 44dp grey circle with the lightning bolt emoji (`⚡`).
  - Render a clean, modern stepped slider matching `Screenshot 2026-10-20 at 1.59.26 PM.png`:
    - Horizontal container with `padding(horizontal = 14.dp)` so edge step labels (`36sp` and `72sp`, or `0s` and `30s`) never clip at the right margin.
    - A crisp `3.dp` horizontal track (`#CBD5E1` inactive background) with an active progress bar filled in **Teal (`#00C4B4`)** from the left edge up to the selected step.
    - A solid **`20.dp` Teal circle thumb (`#00C4B4`)** centered on the active step (inside a `44.dp` touch/drag target).
    - Step labels underneath where the **selected step label is bold Teal (`#00897B`)** and unselected labels are medium Slate (`#475569`), with `maxLines = 1` and `softWrap = false`.
- **Modern Segmented Zone Toggle (`KarooSegmentedPill`)**:
  - A crisp white pill container (`#FFFFFF`, `height = 46.dp`, `RoundedCornerShape(50)`, `1.dp` border `#CBD5E1`, `padding(4.dp)`).
  - Three segments: **`None`**, **`Text`**, and **`Background`** (with proportional weights `0.9f`, `0.9f`, `1.2f` so `"Background"` never truncates).
  - The selected segment is filled with a vibrant **Gold/Amber pill (`#F4C414`)** and bold `#1E2225` text; unselected segments have transparent background and `#475569` medium text.
