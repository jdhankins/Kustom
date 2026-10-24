# Project Rules: Kustom — Hammerhead Karoo Data Fields Extension

## 1. Project Identity & Target Hardware
- You are an expert Kotlin and Android developer building **Kustom**, a custom data fields extension for Hammerhead Karoo cycling computers (Karoo 2 and Karoo 3).
- Extension ID: `"kustom"`. Primary extension service class: `KustomExtension : KarooExtension("kustom", "1")`.
- Target devices have 480x800 px displays. Minimum SDK is API 26. Write 100% idiomatic Kotlin using Coroutines and Flow.

## 2. Strict Karoo Page Setup (`extension_info.xml`) Rules
- ALWAYS use `io.hammerhead:karoo-ext:1.1.9` (`io.hammerhead.karooext.*`).
- **CRITICAL XML FORMAT FOR `res/xml/extension_info.xml`**:
    - Do NOT put `android:` or `app:` namespace prefixes on `<ExtensionInfo>` or `<DataType>` attributes! Karoo OS parses bare attribute names (`id`, `displayName`, `icon`, `scansDevices`, `typeId`, `description`, `graphical`).
    - Every `displayName` and `description` MUST be a `@string/...` reference, and `icon` MUST be a `@drawable/...` vector drawable reference.
    - Title every field's `@string/` display name in `strings.xml` using **`Parent - Child`** format (e.g., `"Power - Power"`, `"Power - Normalized Power"`, `"Heart Rate - Heart Rate"`, `"Speed - Speed"`, `"Cadence - Cadence"`, `"Climb - Grade"`, `"Climb - VAM"`) so they are easy to find under the **Kustom** dropdown in Karoo Page Setup.

## 3. Field Value & Dynamic Label Formatting Rules
- **NUMBERS ONLY (No Units/Verbiage After Numbers)**:
    - NEVER append unit strings (`"W"`, `"km/h"`, `"mph"`, `"bpm"`, `"rpm"`, `"%"`, `"m"`) after a metric value number! Display **ONLY the number** (e.g., `"228"`, `"152"`, `"32.4"`, `"92"`, `"6.5"`).
- **Dynamic Smoothing Label**:
    - When a field supports smoothing and its configured smoothing is `3s`, `5s`, `10s`, or `30s`, dynamically prefix the field's display label with the smoothing window: e.g., if `3s` is selected for `Power`, the label rendered inside the field MUST say **`3s Power`** (or **`5s Power`**, **`10s Power`**, **`30s Power`**). If `0s` is selected, the label says **`Power`**.

## 4. UI Layout & Navigation Rules (`MainActivity`)
- **Keep the Karoo Slate Color Palette**:
    - Screen background: `#DCE1E3`, Card surface: `#F4F6F7`, Primary text: `#1E2225`, Secondary text: `#4A5258`, Slate accent: `#91A6B0`, Visible slider track: `#CFD8DC`, Pill container: `#EEF1F2`.
- **Top Navigation Bar**:
    - Must contain **EXACTLY TWO** choices: **`Global`** and **`Fields`** (in that exact order). Remove `"Logs"` and any other buttons from the top bar.
- **Preview Placement & Narrower Preview Size (`132.dp x 68.dp`)**:
    - **`Global` Tab**: This is the ONLY tab with a high-level preview card at the top of the page.
    - **`Fields` Tab**: Do NOT show a global preview at the top of the `Fields` page. Instead, inside the `Fields` category accordion, **each configurable child metric card** has its own side-by-side header row with the metric's Title + Description on the left (`Modifier.weight(1f).padding(end = 12.dp)`) and its own narrow live preview box (`width = 132.dp, height = 68.dp`) on the right so the description text never wraps mid-word.
- **`Fields` Accordion Filtering & Controls**:
    - List **ALL configurable fields** grouped by category (`Power`, `Heart Rate`, `Speed`, `Cadence`, `Climb`).
    - **Exclude non-configurable fields** from the `Fields` configuration accordion. Never show `"No additional parameters required."`
    - **Working Stepped Smoothing Slider**: Build `KarooSteppedSlider` using an explicit `Row` of 5 clickable/draggable `44.dp` step targets over a visible `#CFD8DC` track so tapping or dragging `0s`, `3s`, `5s`, `10s`, `30s` always updates the smoothing state and the dynamic label (`"3s Power"`) immediately.
    - **Zone Formatting Pill (`None` | `Text` | `Background`)**: Use exact labels **`None`**, **`Text`**, and **`Background`** with proportional weights (`0.85f`, `0.85f`, `1.35f`) and `softWrap = false` so **`Background`** is never truncated to `"Back"`.