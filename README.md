# Kustom - Advanced Custom Data Fields Extension for Hammerhead Karoo

[![Platform](https://img.shields.io/badge/Platform-Android_/_Karoo_OS-00A3FF.svg)](https://hammerhead.io)
[![SDK](https://img.shields.io/badge/Karoo_Ext-1.1.9-132B4C.svg)](https://hammerheadnav.github.io/karoo-ext/)
[![Language](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg)](https://kotlinlang.org/)

**Kustom** is a native Karoo Extension (`io.hammerhead.karooext`) designed for Hammerhead Karoo 2 and Karoo 3 bike computers. It allows cyclists to design, customize, and display high-legibility telemetry fields with dynamic font scaling, custom typography (Karoo RBNo2.1a, Wahoo Barlow, Garmin DIN, Athletic Teko, Clean Roboto), rolling time-window smoothing (3s, 5s, 10s, 30s), manufacturer zone color formatting (Karoo, Garmin, Wahoo, Zwift), and **5 customizable 3-In-One Blocks** with multi-stream corner telemetry, dynamic corner label abbreviations (`AVG CAD`, `MAX CAD`, `AVG TRQ`, `AVG HR`, `AVG SPD`, `DIST`, `ASC`, `DSC`), and unified red sensor status typography (`"No Sensor"` / `"Searching..."`).

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
│  KustomFieldRenderer ◄──► KustomTypefaceResolver ◄──► KustomColorGradeMapper │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     KAROO EXTENSION IPC SERVICE                             │
│  KustomExtension (KarooExtension) ──► KustomDataType (DataTypeImpl)         │
│  ◄──► KarooSystemService (Multi-Stream Telemetry: Power, HR, Speed, Elevation)│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 System Architecture & Data Flow Diagrams

### Component Architecture

```mermaid
graph TD
    subgraph UI ["Presentation Layer"]
        A[MainActivity / Jetpack Compose] --> B[KustomSettingsViewModel]
    end

    subgraph Data ["Data & Repository Layer"]
        B -->|0ms In-Memory Update| C[KustomSettings StateFlow]
        B -->|300ms Debounced Persistence| D[KustomSettingsRepository / DataStore]
        E[KarooSystemRepository] -->|Telemetry Streams| F[KarooSystemService]
    end

    subgraph Domain ["Domain & Canvas Rendering"]
        G[KustomFieldRenderer] --> H[KustomTypefaceResolver]
        G --> I[KustomColorGradeMapper]
        G --> J[RollingTimeSmoother]
    end

    subgraph Extension ["Karoo OS Extension IPC"]
        K[KustomExtension Service] --> L[KustomDataType]
        L -->|startView / Offscreen Bitmap| G
        L -->|startStream / Multi-Stream Combine| E
        L -->|RemoteViews| M[Karoo OS Data Field Cell]
    end
```

### Telemetry Streaming & Offscreen Rendering Sequence

```mermaid
sequenceDiagram
    autonumber
    participant KarooOS as Karoo OS (Ride / Page Editor)
    participant Ext as KustomExtension Service
    participant Type as KustomDataType
    participant Repo as KarooSystemRepository
    participant Render as KustomFieldRenderer

    KarooOS->>Ext: bindService(KAROO_EXTENSION)
    KarooOS->>Type: startView(ViewConfig)
    
    alt config.preview == true (Page Setup)
        Type->>Type: Loop animated mock values (1200ms ticker)
    else config.preview == false (Active Ride)
        Type->>Repo: combine(centerStream, leftStream, rightStream)
        Repo-->>Type: Multi-Stream State (Streaming / Searching / No Sensor)
    end

    Type->>Render: renderComboBlock(config, centerVal, leftVal, rightVal, zoneFormats)
    Render->>Render: Draw Center Primary Zone + Top Left Corner + Top Right Corner + Bottom Gauge Bar
    Render-->>Type: Bitmap
    Type->>KarooOS: emitter.updateView(RemoteViews with Bitmap)
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
│   │   │   ├── FieldCategory.kt       # Parent telemetry categories (Power, Speed, HR, Climb, Time, 3-In-One)
│   │   │   ├── GlobalColorConfig.kt   # Global background colors, power zone count (5/7), & manufacturer palette
│   │   │   ├── GlobalTypographyConfig.kt # Global typography configuration model
│   │   │   ├── KustomFieldCatalog.kt  # Complete 75-field matrix definitions and SDK type mappings
│   │   │   ├── KustomFieldConfig.kt   # Per-field smoothing and zone color configurations
│   │   │   ├── KustomFont.kt          # Top 9 cycling fonts (Karoo, Wahoo, Garmin, Coros, Teko, Roboto...)
│   │   │   ├── KustomSettings.kt      # Root application settings state
│   │   │   ├── SmoothingOption.kt     # Rolling average duration options (0s, 3s, 5s, 10s, 30s)
│   │   │   ├── TypographyConfig.kt    # Render-level typography parameters
│   │   │   ├── ZoneFormat.kt          # Segmented pill option choices (None, Text, Fill, Bar)
│   │   │   └── ZonePalette.kt         # Manufacturer color palettes (Karoo, Garmin, Wahoo, Zwift)
│   │   └── repository/
│   │       ├── KarooSystemRepository.kt   # Reactive flow wrapper for KarooSystemService
│   │       └── KustomSettingsRepository.kt # Asynchronous Preferences DataStore persistence
│   ├── domain/
│   │   ├── KustomColorAndSmoothing.kt # Power/HR/Grade zone color mappers & RollingTimeSmoother
│   │   ├── KustomFieldRenderer.kt    # Offscreen ARGB_8888 canvas renderer (Single Fields & 3-in-1 Combo Blocks)
│   │   └── KustomTypefaceResolver.kt # In-memory Typeface cache & font weight application
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

### 3. Adding a New Data Field to Kustom
To add a new telemetry field (e.g. `TORQUE`):

1. **Add Field Definition to `KustomFieldCatalog.kt`**:
   ```kotlin
   val TORQUE = KustomFieldDefinition(
       typeId = "torque",
       childTitle = "Torque",
       category = FieldCategory.POWER,
       description = "Current torque output",
       supportsSmoothing = true,
       supportsZones = true,
       mockValues = listOf(18.0, 22.0, 25.0, 28.0, 24.0)
   )
   ```
2. **Add String Resources to `res/values/strings.xml`**:
   ```xml
   <string name="field_torque_name">Power - Torque</string>
   <string name="field_torque_desc">Current torque output</string>
   ```
3. **Register Metadata in `res/xml/extension_info.xml`**:
   ```xml
   <DataType
       typeId="torque"
       displayName="@string/field_torque_name"
       description="@string/field_torque_desc"
       graphical="true"
       icon="@drawable/ic_kustom_field" />
   ```
4. **Map SDK Constant in `KustomFieldDefinition.resolveSdkDataTypeId`**:
   ```kotlin
   "torque" -> DataType.Type.TORQUE
   ```

---

## 🔍 Debugging & Log Inspection

Kustom includes an in-memory logcat buffer and an uncaught exception file persistence handler (`kustom_crash.log`).

- **View Live Logs**: Click the **`Logs 📋`** button in `MainActivity` top navigation bar to open the system and crash log dialog.
- **ADB Logcat**: Filter logcat using tag `Kustom`:
  ```bash
  adb logcat -s Kustom
  ```
- **Inspect Service Binding**:
  ```bash
  adb shell dumpsys package com.hank.kustom
  ```

---

## 📜 License & Copyright

Copyright © 2026 Kustom. All rights reserved. Built with the official [Hammerhead Karoo Extension SDK](https://github.com/hammerheadnav/karoo-ext).
