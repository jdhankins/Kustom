# Kustom — Premium Data Fields & Custom Telemetry for Hammerhead Karoo

[![Platform](https://img.shields.io/badge/Platform-Karoo_2_|_Karoo_3-00A3FF.svg)](https://hammerhead.io)
[![SDK](https://img.shields.io/badge/Karoo_Ext-1.1.9-132B4C.svg)](https://hammerheadnav.github.io/karoo-ext/)
[![Download APK](https://img.shields.io/badge/Download-Latest_APK-00E676.svg)](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk)
[![Releases](https://img.shields.io/badge/GitHub-Releases-181717.svg)](https://github.com/jdhankins/Kustom/releases)

**Kustom** is a native Karoo Extension (`io.hammerhead.karooext`) engineered specifically for **Hammerhead Karoo 2** and **Karoo 3** bike computers. Transform your Karoo ride screens with high-visibility, fully customizable data fields designed for maximum outdoor legibility at a glance.

Whether you're executing structured interval workouts, climbing steep alpine passes, or pacing long endurance rides, **Kustom** delivers pixel-exact typography, customizable zone color palettes, multi-metric combo blocks, and real-time telemetry overlays that make your Karoo display truly yours.

<p align="center">
  <img src="docs/images/06_ride_page_kustom.png" width="300" alt="Kustom Fields on Karoo Ride Screen" />
</p>

---

## 🚀 Download & Sideloading Guide

Getting **Kustom** onto your Hammerhead Karoo is quick and straightforward. Choose your device below for step-by-step sideloading instructions.

### 📥 1. Download Latest APK
- **Direct Download**: [app-debug.apk](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk)
- **All Releases**: [GitHub Releases Page](https://github.com/jdhankins/Kustom/releases)

---

### ⚙️ Prerequisites: Enable Developer Options on Karoo
Before sideloading from any platform, enable USB Debugging on your Karoo unit:
1. On your Karoo, go to **Settings $\rightarrow$ About $\rightarrow$ Build Number**.
2. Tap **Build Number** 7 times continuously until the on-screen prompt says `"You are now a developer!"`.
3. Go back to **Settings $\rightarrow$ Developer Options** and toggle **USB Debugging** to **ON**.

---

### 📲 2. Sideloading Instructions by Device / Platform

#### 💻 Option 1: Computer (Mac, Windows, Linux) — Terminal ADB
1. Connect your Karoo 2 or Karoo 3 to your computer using a USB-C data cable.
2. Download [app-debug.apk](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk).
3. Open Terminal (Mac/Linux) or Command Prompt / PowerShell (Windows) and run:
   ```bash
   # Confirm device connection
   adb devices

   # Sideload Kustom onto Karoo
   adb install -r app-debug.apk
   ```
4. On your Karoo screen, authorize the USB debugging prompt if requested.

---

#### 🌐 Option 2: Computer or Tablet — Web ADB (No Command Line Needed)
Works on Chrome, Edge, Brave, or Opera browsers without installing ADB tools:
1. Download [app-debug.apk](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk) to your computer.
2. Connect your Karoo via USB cable.
3. Open [webadb.js.org](https://webadb.js.org/) in your browser.
4. Click **Start $\rightarrow$ Add Device**, select your Karoo unit, and authorize connection on your Karoo screen.
5. Click **Install APK**, select the downloaded `app-debug.apk`, and wait for installation to complete.

---

#### 📱 Option 3: Android Smartphone (via Bugjaeger & USB-C Cable)
You can sideload **Kustom** directly from an Android phone without needing a computer!
1. Download [app-debug.apk](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk) to your Android phone's Downloads folder.
2. Install **Bugjaeger Mobile ADB** (free on Google Play Store) on your phone.
3. Connect your Android phone directly to your Karoo using a USB-C to USB-C cable (OTG).
4. Open **Bugjaeger** on your phone, accept USB permission prompts, tap the **Packages** tab, tap the **`+`** icon, select **Select APK File**, choose `app-debug.apk`, and tap **Install**.

---

#### 🍎 Option 4: iPhone / iPad (iOS) — Web ADB via Wi-Fi or USB-C
1. Download [app-debug.apk](https://github.com/jdhankins/Kustom/releases/latest/download/app-debug.apk) to the **Files** app on your iPhone or iPad.
2. **For USB-C iPad / iPhone 15+**: Connect your iPhone/iPad directly to your Karoo using a USB-C cable.
   **For Wireless ADB**: Enable **Wireless Debugging** under Karoo **Developer Options**, note the IP address and Port, and connect Karoo to the same Wi-Fi network as your iPhone.
3. Open [webadb.js.org](https://webadb.js.org/) in Safari or Chrome on iOS.
4. Tap **Connect** (or connect via Web ADB Wireless IP), authorize connection on Karoo, tap **Install APK**, and choose `app-debug.apk` from Files.

---

#### 🚴 Option 5: Directly on Karoo Unit (Native On-Device Installation)
If you have a Web Browser app (e.g. Chrome, Kiwi) or a File Manager app (e.g. Amaze, AnExplorer) installed on your Karoo:
1. Open the web browser on your Karoo and navigate to [github.com/jdhankins/Kustom/releases](https://github.com/jdhankins/Kustom/releases).
2. Download `app-debug.apk` directly to Karoo storage.
3. Open your File Manager app on Karoo, navigate to `/sdcard/Download/`, tap `app-debug.apk`, and select **Install**.

---

## ✨ Feature Showcase

### 🎨 1. Universal Typography & Outdoor Visibility
Customize every label and value with popular cycling typefaces and outdoor-optimized contrast:
- **9 Cycling Typefaces**: **Wahoo Barlow**, **Garmin DIN**, **Karoo RBNo2.1a**, **Coros Dura**, **Athletic Teko**, **Clean Roboto**, **Eurostile**, **Oswald**, and **Monospace**.
- **High-Contrast Outdoor Visibility**: Pure black/white header labels anchored to unified top cap-lines for maximum readability in direct sunlight.
- **Wahoo ELEMNT Superscript Formatting**: Trailing seconds and decimal digits render in clean superscript (`2:00:⁰⁰`, `10:⁰⁰`, `-3.⁴%`).
- **Custom Units & Conversions**: Automatic $m/s \rightarrow m/h$ VAM climbing speed conversions, negative grade superscripts (`-3.4%`), and metric/imperial support.

<p align="center">
  <img src="docs/images/01_global_settings.png" width="280" alt="Kustom Global Typography Settings" />
</p>

---

### 🎨 2. Multiple Zone Color Palettes & Customization
Tailor your ride display to match your preferred eco-system or visual style with support for **multiple manufacturer zone color palettes**:
- **5 Zone Color Palettes**: Choose between **Karoo**, **Garmin**, **Wahoo**, **Zwift**, and **Neon Glass**.
- **Power & Heart Rate Zones**: Select 5-zone or 7-zone color modes for Power output, Heart Rate, and Elevation Grade % metrics.
- **Flexible Zone Color Modes**: Apply zone colors as **Full Card Background Fills**, **Text Color Accents**, or **Bottom Gauge Bars**.
- **3D "Neon Glass" Shader Option**: Enjoy an optional 3-pass glossy liquid-glass shader (`ZonePalette.NEON_GLASS`) featuring vertical neon gradients, top specular glass reflection, and luminous neon rims.

<p align="center">
  <img src="docs/images/05_zone_palette_config.png" width="260" alt="Kustom Zone Palette Configuration" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/images/04_field_background_config.png" width="260" alt="Kustom Field Background Configuration" />
</p>

---

### 🧩 3. Full-Width 3-In-One Composite Blocks & Field Customization
Consolidate **3 metrics on 1 card** to maximize screen efficiency:
- **Center Hero Primary Metric**: Large, bold main digits with auto-scaling font sizes.
- **Top-Left & Top-Right Stacked Metrics**: Corner labels and values for NP, Avg HR, Max Power, TSS, W/kg, Cadence, Grade %, etc.
- **Bottom 7-Zone Gauge Bar**: Live power/HR zone bar showing your current intensity zone.
- **Universal Customization**: Pick ANY metric from Kustom's full 76+ catalog for the Center, Left, and Right slots!

<p align="center">
  <img src="docs/images/02_fields_category_list.png" width="260" alt="Kustom Field Category Accordion" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/images/03_power_field_config.png" width="260" alt="Kustom Individual Field Configuration" />
</p>

---

### 🎯 4. Target Power Corridor (Experimental)
A specialized pacing display for structured training rides and workouts:
- **Live Target Range**: Displays your target watt corridor (e.g., **`117-146W`**).
- **Target Cursor Track**: Horizontal track capsule with a real-time needle indicating whether you are on target, below target, or above target.
- **State Badges**: High-contrast status badges (**`ON TARGET`**, **`BELOW TARGET`**, **`ABOVE TARGET`**) for instant feedback.

---

### 📊 5. Workout Profile Graph HUD (Experimental)
An interactive workout profile graph for structured `.fit` and `.zwo` workouts:
- **Local FIT & ZWO Parsing**: Automatically parses active workout files from TrainingPeaks, TrainerRoad, and Zwift.
- **Multi-Trace Telemetry Overlays**: 3 independent toggleable live trace lines for **Power**, **Heart Rate**, and **Cadence** rendered using smooth cubic Bézier curves.
- **Live Progress Line & Cursor Pin**: A vertical progress line tracks your exact workout position in real-time.

---

### ⚠️ Known Karoo SDK (`karoo-ext`) Workout Stream Limitations
> [!NOTE]
> The current Hammerhead Karoo Extensions SDK (`karoo-ext:1.1.9`) does not yet broadcast `OnWorkoutLoaded`, `OnWorkoutUnloaded`, or `OnStepSkipped` lifecycle events to third-party extensions.
> As a result, standalone workout text timers were removed in favor of pure live sensor telemetry. The visual **Target Corridor** and **Workout Graph** HUDs are marked **Experimental**: they sync live when an active workout ride is running, but may experience lag pre-ride or drift if workout intervals are manually skipped. Full 1:1 instant sync will be enabled as soon as Hammerhead expands the `karoo-ext` workout event API.

---

## 🛠️ Developer Guide (Building & Maintaining)

For developers wanting to build **Kustom** from source or contribute to the repository:

### 1. Prerequisites
- **Android Studio**: Ladybug or newer.
- **JDK**: Java 17+.
- **Android SDK**: API Level 34+ (Build-Tools 35.0.0).
- **Target Device**: Hammerhead Karoo 2 or Karoo 3 unit (or Android Karoo Emulator).

### 2. Build & Deploy Commands
```bash
# Clone repository
git clone https://github.com/jdhankins/Kustom.git
cd Kustom

# Build Debug APK
./gradlew app:assembleDebug

# Deploy to connected Karoo unit
adb -s <device_serial> install -r app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity Configuration UI
adb -s <device_serial> shell am start -n com.hank.kustom/.ui.MainActivity
```

---

## 📜 License & Copyright

Copyright © 2026 Kustom. All rights reserved. Built with the official [Hammerhead Karoo Extension SDK](https://github.com/hammerheadnav/karoo-ext).
