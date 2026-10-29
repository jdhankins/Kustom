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

## 2. STRICT LIVE-RIDE vs. MOCK-PREVIEW ISOLATION (Zero Leaked Mock Data in Rides)
- NEVER Leak Mock/Fallback Values When isMock == false:
    - Mock sample numbers ("117-146", "152-168", 214.0 watts, 142.0 watts, etc.) are ONLY permitted when isMock == true (in-app preview or page editor preview).
    - During an actual ride (isMock == false):
        1. NEVER fall back to "117-146" or WorkoutFileParser.resolveActiveWorkoutFile(...) for target power ranges when Karoo's live workout target stream (extractTargetPowerString(targetDataPoint)) is null.
        2. On a 3-In-One Combo Block where Center is set to Workout Target ("workout-target-power" or "workout-primary-target"): if isMock == false and extractTargetPowerString(targetDataPoint) == null (no workout is running),comboCenterLabel MUST fall back to KustomFieldCatalog.POWER.resolveDynamicLabel(resolvedConfig.smoothingOption) (e.g., "3S POWER" or "POWER").
        3. On Target Power Corridor ("workout-target-corridor"): if isMock == false and no live workout target is streaming from targetDataPoint, leftSlot.value MUST be "--" (not a parsed file from disk), and the corridor bar must not show a fake green range.
        4. In KustomFieldRenderer.kt, NEVER use powerValueWatts ?: 214.0 when mappedColor is null. If mappedColor is null or isSensorStatusText is true, activeZoneColor MUST be null, and when backgroundColor is not filled with a zone color, defaultCornerValueColor and all text colors MUST use the theme's standard contrast color (dark charcoal in Day Mode, white in Night Mode) so a card never renders white text on a white background!

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
    - Reference Digit Height ("88") for Vertical Measurement in drawTextInBox: Always measure vertical height (refBounds.height()) and vertical baseline (refBounds.top / refBounds.exactCenterY()) using paint.getTextBounds("88", 0, 2, refBounds) while measuring renderText for horizontal width (widthBounds.width()). This prevents low-height characters like "--" from blowing up to 100px font size and bloating into solid black rectangles.

## 4. Workout Graph Field & FIT/ZWO Storage Rules
- Mandatory In-Memory Caching in WorkoutFileParser:
    - NEVER re-scan directories or re-parse .fit/.zwo files on every 1 Hz frame. Cache the parsed ParsedWorkout keyed by (filePath, lastModified, ftp) and throttle directory timestamp checks to at most once every 10 seconds.
    - Always wrap FileInputStream in BufferedInputStream(..., 8192).use { ... }.
- Permissions: Keep <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" /> in AndroidManifest.xml.

## 5. True 1:1 Font Scaling, State Isolation & karoo-ext Lifecycle Rules
- NEVER Stretch Fonts: Keep paint.textScaleX = 1.0f and use ContentScale.Fit with exact container pixel dimensions on all preview Image composables.
- State Isolation: Always use rememberUpdatedState inside custom gesture controls (KarooSteppedSlider) and atomic DataStore updates in KustomSettingsRepository.
- Strict karoo-ext Cleanup: Always emit UpdateGraphicConfig(showHeader = false) at the start of startView, and cancel all coroutines/consumers inside emitter.setCancellable { job.cancel() }.
