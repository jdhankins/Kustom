# Kustom - Advanced Custom Data Fields Extension for Hammerhead Karoo

[![Platform](https://img.shields.io/badge/Platform-Android_/_Karoo_OS-00A3FF.svg)](https://hammerhead.io)
[![SDK](https://img.shields.io/badge/Karoo_Ext-1.1.9-132B4C.svg)](https://hammerheadnav.github.io/karoo-ext/)
[![Language](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg)](https://kotlinlang.org/)

**Kustom** is the ultimate native Karoo Extension (`io.hammerhead.karooext`) built specifically for Hammerhead Karoo 2 and Karoo 3 bike computers. Elevate your ride experience with high-visibility custom telemetry displays engineered for maximum legibility at a glance. Enjoy pixel-exact font scaling, premium popular typography, customizable rolling time-window smoothing, popular zone color palettes, an interactive **Workout Profile Graph HUD**, and **5 versatile 3-In-One Composite Blocks** featuring concurrent multi-stream telemetry, intelligent corner label abbreviations, and unified high-contrast sensor status indicators.

---

## ✨ Key Feature Highlights

### 📊 1. Workout Profile Graph HUD
- **Local FIT & ZWO Workout Parsing**: Automatically resolves and parses active structured workout files (`.fit` and `.zwo`) loaded on device storage from platforms like TrainingPeaks, TrainerRoad, and Zwift.
- **Flat-Bottom Solid Zone Blocks**: Renders 100% solid zone-colored target blocks with flat-bottom baselines and rounded top corners for clean edge-to-edge layout filling.
- **Real-Time Progress Line & Cursor Pin**: A vertical progress line with a top cursor pin tracks your exact workout progress in real-time. Pre-ride, the progress line rests at 0% with full block opacity.
- **Multi-Trace Telemetry Overlays**: 3 independent toggleable live trace lines for **Power (White/Black)**, **Heart Rate (Red)**, and **Cadence (Cyan)** rendered using smooth cubic Bézier curve paths with adaptive Light/Dark mode contrast borders.

### 🧩 2. 5 Customizable 3-In-One Composite Blocks (`3-in-1 - Block 1` .. `Block 5`)
- **Multi-Slot Composite HUD**: Consolidates 3 telemetry metrics into a single screen cell (Center Primary Metric + Top-Left Corner Metric + Top-Right Corner Metric + Bottom 5/7-Zone Gauge Bar).
- **Workout Target Center Metric**: Selecting **Workout Target** as the center metric dynamically displays the active workout target range (e.g. `117-146`) in the center header label position, while displaying live smoothed Power output in the main primary value digits!
- **Dynamic Corner Label Abbreviations**: Automatically abbreviates long corner titles (`AVG CAD`, `MAX CAD`, `AVG TRQ`, `AVG HR`, `AVG SPD`, `DIST`, `ASC`, `DSC`) with text width clamping to prevent label crowding.
- **Per-Slot Customization**: Independent rolling smoothing (`0s`–`30s`) and per-slot zone color modes (`None`, `Text`, `Fill`, `Bar`) for Center, Left, and Right metrics. Includes **TSS** as a selectable corner metric option.

### 🎨 3. Typography & Manufacturer Zone Palettes
- **9 Cycling Typefaces**: Choose from **Karoo RBNo2.1a**, **Wahoo Barlow**, **Garmin DIN**, **Coros Dura**, **Athletic Teko**, **Clean Roboto**, **Eurostile**, **Oswald**, and **Monospace**.
- **4 Manufacturer Zone Color Palettes**: Toggle between **Karoo**, **Garmin**, **Wahoo**, and **Zwift** zone color systems for power and heart rate fields.
- **Unified Sensor Status Typography**: Displays bold red (`0xFFFF5252`) `"No Sensor"` or `"Searching..."` across all standard and 3-in-1 composite fields when paired sensors are disconnected or searching.

---

## 🏛️ Architectural Overview

The Kustom extension follows **Android Clean Architecture** principles, cleanly separating UI presentation, domain canvas rendering, local settings persistence, and Karoo OS IPC telemetry streaming.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER (UI)                            │
│  MainActivity.kt + Jetpack Compose (KustomKarooNativeScreen, GlobalFonts)   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VIEWMODEL & DATA LAYER                              │
│  KustomSettingsViewModel ◄──► KustomSettingsRepository (Preferences DataStore)
│  CustomComboInstance (5 Customizable 3-in-1 Composite Blocks)                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DOMAIN & RENDERING ENGINE                            │
│  KustomFieldRenderer ◄──► WorkoutFileParser ◄──► KustomTypefaceResolver    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     KAROO EXTENSION IPC SERVICE                             │
│  KustomExtension (KarooExtension) ──► KustomDataType (DataTypeImpl)         │
│  ◄──► KarooSystemService (Multi-Stream Telemetry: Power, HR, Speed, Workout)│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📂 Project Structure Walkthrough

```
app/src/main/
├── java/com/hank/kustom/
│   ├── data/
│   │   ├── model/
│   │   │   ├── ColorMode.kt           # Zone formatting color modes (NONE, BACKGROUND, TEXT)
│   │   │   ├── CustomComboInstance.kt # User-configurable 3-in-1 combo block instance model
│   │   │   ├── FieldCategory.kt       # Parent telemetry categories (Power, Speed, HR, Climb, Time, 3-In-One, Workout)
│   │   │   ├── GlobalColorConfig.kt   # Global background colors, power zone count (5/7), & manufacturer palette
│   │   │   ├── GlobalTypographyConfig.kt # Global typography configuration model
│   │   │   ├── KustomFieldCatalog.kt  # Complete 76-field matrix definitions and SDK type mappings
│   │   │   ├── KustomFieldConfig.kt   # Per-field smoothing, zone color, and trace line configurations
│   │   │   ├── KustomFont.kt          # Top 9 cycling fonts (Karoo, Wahoo, Garmin, Coros, Teko, Roboto...)
│   │   │   ├── KustomSettings.kt      # Root application settings state
│   │   │   ├── ParsedWorkout.kt       # Normalized workout step and profile model
│   │   │   ├── SmoothingOption.kt     # Rolling average duration options (0s, 3s, 5s, 10s, 30s)
│   │   │   ├── TypographyConfig.kt    # Render-level typography parameters
│   │   │   ├── ZoneFormat.kt          # Segmented pill option choices (None, Text, Fill, Bar)
│   │   │   └── ZonePalette.kt         # Manufacturer color palettes (Karoo, Garmin, Wahoo, Zwift)
│   │   └── repository/
│   │       ├── KarooSystemRepository.kt   # Reactive flow wrapper for KarooSystemService
│   │       └── KustomSettingsRepository.kt # Asynchronous Preferences DataStore persistence
│   ├── domain/
│   │   ├── KustomColorAndSmoothing.kt # Power/HR/Grade zone color mappers & RollingTimeSmoother
│   │   ├── KustomFieldRenderer.kt    # Offscreen ARGB_8888 canvas renderer (Single Fields, 3-in-1 Blocks, Workout Graph)
│   │   ├── KustomTypefaceResolver.kt # In-memory Typeface cache & font weight application
│   │   └── WorkoutFileParser.kt      # Throttled local FIT/ZWO workout file parser & in-memory cache
│   ├── extension/
│   │   ├── KustomExtension.kt        # Primary KarooExtension service entry point
│   │   └── types/
│   │       └── KustomDataType.kt     # Concrete DataTypeImpl handling IPC views and multi-stream flows
│   ├── ui/
│   │   ├── KustomSettingsViewModel.kt# ViewModel with 0ms in-memory state & DataStore persistence
│   │   └── MainActivity.kt           # Configuration UI activity with adaptive Light/Dark theme
│   └── util/
│       └── KustomLogger.kt           # Logcat buffer and disk crash log persistence
└── res/
    ├── drawable/
    │   ├── ic_category_*.xml         # Category icons for fields accordion tabs
    │   └── ic_kustom_field.xml       # Vector logo & icon asset
    ├── values/
    │   └── strings.xml               # App string resources & "Parent - Child" page setup titles
    └── xml/
        └── extension_info.xml        # Karoo OS metadata manifest declaring all extension data types
```

---

## 🛠️ Developer Guide: Learning & Maintaining Kustom

### 1. Prerequisites & Environment
- **Android Studio**: Android Studio Ladybug or newer.
- **JDK**: Java 17+.
- **Android SDK**: API Level 34+ (Build-Tools 35.0.0).
- **Target Device**: Hammerhead Karoo 2 or Karoo 3 unit connected via USB (or Karoo Emulator).

### 2. How to Build & Install
Run the Gradle assemble task and deploy to your connected device:

```bash
# Build Debug APK
./gradlew app:assembleDebug

# Deploy to connected Karoo unit
adb -s <device_serial> install -r app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity Settings
adb -s <device_serial> shell am start -n com.hank.kustom/.ui.MainActivity
```

---

## 📜 License & Copyright

Copyright © 2026 Kustom. All rights reserved. Built with the official [Hammerhead Karoo Extension SDK](https://github.com/hammerheadnav/karoo-ext).
