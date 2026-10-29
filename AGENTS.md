\# Project Rules: Kustom (com.hank.kustom) — Hammerhead Karoo Data Fields Extension

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

## 2. Centralized Full-Width Layout Engine (FullWidthLayoutEngine.kt)
- Single Layout Engine: All full-width composite fields (renderComboBlock, renderTargetPowerCorridor, and future full-width HUDs) MUST use FullWidthLayoutEngine.
- Strict Non-Overlapping Horizontal Columns (Sum = 100%):
    - Left Column: 3.5% to 28.5% (25% width), strictly left-aligned (Paint.Align.LEFT).
    - Left Gutter: 28.5% to 30.5% (2.0% empty buffer).
    - Center Hero Column: 30.5% to 69.5% (39% width when side slots are present; 8.0% to 92.0% when both side slots are Empty).
    - Right Gutter: 69.5% to 71.5% (2.0% empty buffer).
    - Right Column: 71.5% to 96.5% (25% width), right-aligned (Paint.Align.RIGHT) when plain text, or centered inside its 71.5%..96.5% box when isZoneFilled == true.
- LABELS NEVER AUTO-SCALE; ONLY VALUES AUTO-SCALE:
    - Labels (leftSlot.label, centerHero.label, rightSlot.label) MUST NEVER be passed through a shrinking while loop. All labels across every card (whether a metric abbreviation like "AVG SPD", "DIST", "TARGET", "STATUS", "3S POWER", or a center workout target range like "117-146") must be drawn via drawFixedLabel at the exact same fixed pixel size fixedLabelPx (computed strictly from labelTypography.fontSizeSp) and top-anchored to labelTopY = height * 0.07f.
    - Only Values (leftSlot.value, rightSlot.value, centerHero.value) auto-scale via drawTextInBox to fit their bounding box.
    - Top-Anchor Corner Values: Left and Right corner values must be top-anchored (drawY = valueBoxTop - bounds.top + halfStroke at valueBoxTop = height * 0.25f) so shrunken multi-character values (like "115-160") maintain the exact same vertical gap below their label as 3-character values ("140", "7.7").
    - Synchronize Left & Right Corner Value Sizes on 3-In-One Cards: When both Left and Right slots are numeric metrics (not a range string with '-') and neither is a status text badge, synchronize their fitted value text size (minOf(leftValSize, rightValSize)) so "18.1" and "7.7" match in height.
- Mandatory Stroke-Width Rescaling Inside drawTextInBox:
    - Whenever drawTextInBox steps down paint.textSize for a value in its while loop, it MUST proportionally step down paint.strokeWidth = size * strokeRatio (capped at 0.028f for weight 900) and include paint.strokeWidth in bounds.width() and bounds.height().
- Target Power Corridor Design (workout-target-corridor):
    - Left Slot: HeaderSlot.MetricSlot(label = "TARGET", value = "115-160"), strictly top-left aligned (Paint.Align.LEFT at leftColStart, topY = height * 0.07f for label, valueBoxTop = height * 0.25f for value—identical to all 3-in-1 left slots).
    - Right Slot: Uses a zone-filled 2-line HeaderSlot.MetricSlot(label = statusLabel, value = statusValue, zoneColor = stateColor, isZoneFilled = true) spanning the full 2-line corner height (height * 0.04f to height * 0.58f), where statusValue is "✓ OK" in Matte Green (#2E7D32) when on target, "▲+18" (delta watts to minTarget) in Matte Blue (#2563EB) when low, and "▼-15" (delta watts above maxTarget) in Matte Red (#C62828) when high.
    - Bottom Track: Do NOT draw tiny unreadable min/max numbers on the corridor bar (since the target range is already displayed in the Left Slot). Instead, draw crisp vertical gate tick marks at tLeftX and tRightX framing the green target corridor box, plus the white cursor needle.

## 3. 3-In-One Configurable Blocks & Workout Target Center Alignment Rules
- Static 3-In-One Blocks: combo-block-1 through combo-block-5 are pre-registered in res/xml/extension_info.xml and KustomFieldCatalog.
- Center-Only Workout Target Logic:
    - Workout target metrics ("workout-target-power", "workout-primary-target") MUST ONLY be selectable in the Center position of a 3-in-1 block (never in Left or Right corner dropdowns).
    - When a workout target metric is selected in the Center position of a 3-in-1 block:
        1. Top Center Label (comboCenterLabel): Dynamically renders the active workout target range or wattage extracted from DataType.Type.WORKOUT_POWER_TARGET / WORKOUT_PRIMARY_TARGET (e.g., "117-146", falling back to "117-146" in preview/mock mode).
        2. Primary Center Value (primaryValText): Streams live Power telemetry (KustomFieldCatalog.POWER.resolveSdkDataTypeId(smoothing)), displaying live watts when paired or "No Sensor" in red when unpaired.
- Explicit DataType.Field Extraction (No Blanket Loops):
    - Never use val targetDp = priDp ?: pDp. Always evaluate extractTargetPowerString(pDp) ?: extractTargetPowerString(priDp) using explicit DataType.Field keys (WORKOUT_TARGET_MIN_VALUE, WORKOUT_TARGET_MAX_VALUE, WORKOUT_TARGET_VALUE, WORKOUT_TARGET_OUTPUT_VALUE, SINGLE).

## 4. Workout Graph Field & FIT/ZWO Storage Rules
- Mandatory In-Memory Caching in WorkoutFileParser:
    - NEVER re-scan directories or re-parse .fit/.zwo files on every 1 Hz frame. Cache the parsed ParsedWorkout keyed by (filePath, lastModified, ftp) and throttle directory timestamp checks to at most once every 10 seconds.
    - Always wrap FileInputStream in BufferedInputStream(..., 8192).use { ... }.
- Permissions: Keep <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" /> in AndroidManifest.xml for Karoo 3 Scoped Storage compatibility.
- Multi-Trace Stream Subscription:
    - For "workout-graph", KustomDataType must subscribe concurrently to POWER, HEART_RATE, and CADENCE streams (plus workout elapsed/remaining duration) and pass immutable list snapshots (.toList()) to KustomFieldRenderer.renderWorkoutGraph.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
