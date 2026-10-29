# Project Rules: Kustom (`com.hank.kustom`) — Hammerhead Karoo Data Fields Extension

## 1. Project Identity, Package Structure & Gradle SDK Alignment
- Package namespace: `com.hank.kustom`. Extension ID: `"kustom"`.
- Primary extension service: `com.hank.kustom.extension.KustomExtension : KarooExtension("kustom", "1")`.
- Keep `minSdk = 26` and `targetSdk = 34` in `app/build.gradle.kts`.

## 2. Centralized Full-Width Layout Engine (`FullWidthLayoutEngine.kt`)
- **Single Layout Engine**: All full-width composite fields (`renderComboBlock`, `renderTargetPowerCorridor`, and future full-width HUDs) MUST use `FullWidthLayoutEngine`.
- **Strict Non-Overlapping Horizontal Columns (Sum = 100%)**:
  - Left Column: `3.5%` to `26.5%` (`23%` width).
  - Left Gutter: `26.5%` to `29.0%` (`2.5%` empty buffer).
  - Center Hero Column: `29.0%` to `71.0%` (`42%` width when side slots are present; `8.0%` to `92.0%` when both side slots are `Empty`).
  - Right Gutter: `71.0%` to `73.5%` (`2.5%` empty buffer).
  - Right Column: `73.5%` to `96.5%` (`23%` width; `StatusPill` spans `69.0%` to `96.5%` in the top `6%..30%` band).
- **Use `Paint.getTextBounds` (`drawTextInBox`), NEVER `descent - ascent` for Box Fitting**:
  - Always fit and center text inside its target `[boxLeft, boxTop, boxRight, boxBottom]` rectangle using `paint.getTextBounds(text, 0, text.length, bounds)` and `bounds.exactCenterX()` / `bounds.exactCenterY()` so internal font ascent/descent padding never shrinks `72sp` fonts or causes vertical baseline drift.
- **Unified Footer Band (`78%..92%` Height)**:
  - Both the 7-Zone Bar and the Target Corridor Bar occupy `top = height * 0.78f` to `bottom = height * 0.92f`.
  - Target Corridor boundary numbers (`minTarget`, `maxTarget`) are drawn vertically centered on the track at `tLeftX - 8f` (`Align.RIGHT`) and `tRightX + 8f` (`Align.LEFT`)—never above `trackTop`.

## 3. True 1:1 Font Scaling, State Isolation & `karoo-ext` Lifecycle Rules
- **NEVER Stretch Fonts**: Keep `paint.textScaleX = 1.0f` and use `ContentScale.Fit` with exact container pixel dimensions on all preview `Image` composables.
- **State Isolation**: Always use `rememberUpdatedState` inside custom gesture controls (`KarooSteppedSlider`) and atomic DataStore updates in `KustomSettingsRepository`.
- **Strict `karoo-ext` Cleanup**: Always emit `UpdateGraphicConfig(showHeader = false)` at the start of `startView`, and cancel all coroutines/consumers inside `emitter.setCancellable { job.cancel() }`.
