# Kustom — Premium Data Fields & Custom Telemetry for Hammerhead Karoo

[![Platform](https://img.shields.io/badge/Platform-Karoo_2_|_Karoo_3-00A3FF.svg)](https://hammerhead.io)
[![SDK](https://img.shields.io/badge/Karoo_Ext-1.1.9-132B4C.svg)](https://hammerheadnav.github.io/karoo-ext/)
[![Download APK](https://img.shields.io/badge/Download-Latest_APK-00E676.svg)](https://raw.githubusercontent.com/jdhankins/Kustom/main/releases/app-debug.apk)
[![Releases](https://img.shields.io/badge/GitHub-Releases-181717.svg)](https://github.com/jdhankins/Kustom/releases)

**Kustom** is a native Karoo Extension (`io.hammerhead.karooext`) engineered specifically for **Hammerhead Karoo 2** and **Karoo 3** cycling computers. Transform your Karoo ride screens with high-visibility, fully customizable data fields designed for instant outdoor legibility at speed.

Whether you're climbing steep alpine passes, or pacing long endurance rides, **Kustom** delivers pixel-exact typography, customizable zone color palettes, multi-metric composite blocks, and real-time telemetry overlays that make your Karoo display truly yours.

---

## 📸 Screen Showcase

| High-Contrast Day Mode | Neon Glass & Zone Fills | Multi-Metric Full Width | Target Corridor & Graphs |
| :---: | :---: | :---: | :---: |
| <img width="230" alt="High-Contrast Day Mode" src="https://github.com/user-attachments/assets/486d2b58-3d64-4ba7-9fa2-f69fb223dd76" /> | <img width="230" alt="Neon Glass Zone Fill" src="https://github.com/user-attachments/assets/54e7420a-6e16-4b54-95da-509852f64e21" /> | <img width="230" alt="Multi-Metric Full Width" src="https://github.com/user-attachments/assets/09f4ad68-cf17-4a83-a290-9024694a7313" /> |  <img width="230" alt="Target Corridor" src="https://github.com/user-attachments/assets/8a5bd915-c899-4790-9e88-cf34ec7c03fa" /> |

---

## ✨ Feature Highlights

### 🎨 1. Universal Cycling Typography & Outdoor Contrast
Customize every label and value with authentic cycling typefaces and outdoor-optimized contrast:
- **Popular Cycling Fonts**: Choose between **Karoo (RBNo2.1a)**, **Wahoo (Barlow)**, **Wahoo 2 (Barlow SemiCondensed)**, **Garmin (DIN 1451)**, **Garmin 2 (Highway Gothic)**, **Coros Dura (Barlow Condensed)**, **Oswald**, **Teko**, and **Roboto (Clean)**.
- **Fixed Non-Scaling Labels**: Header labels (`AVG HR`, `TARGET`, `AVG SPD`, `DIST`) are anchored to a uniform top cap-line at calibrated pixel heights, ensuring labels never shrink or look mismatched on the same row.
- **Karoo-Native Superscript Decimals**: Decimals like `18.6` automatically render as primary digits (`18`) with a high-visibility superscript (`⁶`), matching Karoo's native `SPEED 20¹` typography.

### 🌈 2. Multiple Zone Palettes & "Neon Glass" 3D Shaders
Tailor your ride display to match your favorite bike computer ecosystem or visual style:
- **5 Zone Color Palettes**: Choose between **Karoo**, **Garmin**, **Wahoo**, **Zwift**, and **Neon Glass**.
- **Liquid-Glass "Neon Glass" Shader**: A proprietary 3-pass Canvas shader (vertical neon gradient, curved specular top glass reflection, and luminous rim border) that brings 3D liquid-glass styling to your 7-zone bars, target corridor, and card backgrounds.
- **Flexible Zone Color Modes**: Apply zone colors as **Full Card Background Fills**, **Text Color Accents**, or **Subtle 7-Zone Gauges**.

<img width="270" height="424" alt="Global Settings Swatch Preview" src="https://github.com/user-attachments/assets/4067cf63-5df3-4eb0-b74e-5ba89c09c3fa">

### 🧩 3. Full-Width 3-In-One Composite Blocks
Consolidate **3 metrics onto 1 full-width card** to maximize screen efficiency without visual clutter:
- **Center Hero Metric**: Large, bold main digits with auto-scaling font sizes up to 72sp.
- **Left & Right Stacked Metrics**: Corner labels and values for NP, Avg HR, Max Power, TSS, W/kg, Cadence, VAM, Grade, Distance, and more.
- **Integrated 7-Zone Gauge Bar**: Live power or heart rate zone indicator tracking your current intensity zone.
- **Universal Customization**: Configure ANY metric from Kustom's full catalog for the Center, Left, and Right slots across 5 customizable blocks.

<img width="285" height="220" alt="3-In-One Composite Block" src="https://github.com/user-attachments/assets/c4864757-f6eb-44c1-9257-239634d3b652">

### 🎯 4. Target Power Corridor (Experimental)
A specialized pacing display engineered for structured workouts and interval efforts:
- **Target Range Corridor**: Displays your target power range (e.g., **115-160W**) in clear, high-contrast numbers.
- **Dynamic Needle Track**: A horizontal corridor track with a color-shifting needle indicating whether you are on target, under, or over your target watts.
- **Instant Status Badge**: High-contrast corner badge displaying **✓ OK** (Matte Green), **▲+18** (Matte Blue push-up delta), or **▼-15** (Matte Red ease-off delta).

<img width="273" height="90" alt="Target Power Corridor" src="https://github.com/user-attachments/assets/914041d5-bc44-4869-93e1-450917f6c770">

### 📊 5. Workout Profile Graph HUD (Experimental)
An interactive workout profile graph for structured `.fit` and `.zwo` workouts:
- **Local FIT & ZWO Parsing**: Automatically parses active workout files from TrainingPeaks, TrainerRoad, and Zwift.
- **Multi-Trace Telemetry Overlays**: 3 independent toggleable live trace lines for **Power**, **Heart Rate**, and **Cadence**.
- **Live Progress Line**: A vertical playhead line tracks your exact workout position in real time.

<img width="256" height="354" alt="Workout Profile Graph HUD" src="https://github.com/user-attachments/assets/8172ce34-7ce0-482f-8a4e-1b8314bc1b56">

### ⚠️ Known Karoo SDK (`karoo-ext`) Workout Stream Limitations
> [!NOTE]
> The current Hammerhead Karoo Extensions SDK (`karoo-ext:1.1.9`) does not yet broadcast `OnWorkoutLoaded`, `OnWorkoutStarted`, or interval step events to third-party extensions.
> As a result, standalone workout text timers were removed in favor of pure live sensor telemetry. The visual **Target Power Corridor** and **Workout Graph HUD** remain available as experimental features during active workouts!

---

## 🚀 How to Add Kustom Fields to Your Karoo Profile

Once sideloaded onto your Karoo, adding Kustom fields to any profile is seamless:
1. Turn on your Karoo and go to **Settings $\rightarrow$ Profiles**.
2. Select your profile, tap **Edit Page**, and tap an empty slot (or tap an existing field to replace it).
3. Tap **+ Add Field**, scroll to the bottom of the category list (or swipe the top category filter bar to the far right), and tap **Kustom**.
4. Select your desired 3-in-One block, Target Corridor, or single-metric field!

---

## 📥 Sideloading Guide

Getting **Kustom** onto your Hammerhead Karoo takes only a couple of minutes. Choose your preferred method below:

### 📥 1. Download Latest APK
- **Direct Download**: [app-debug.apk](https://raw.githubusercontent.com/jdhankins/Kustom/main/releases/app-debug.apk)
- **All Releases**: [GitHub Releases Page](https://github.com/jdhankins/Kustom/releases)

### ⚙️ Prerequisites: Enable Developer Options on Karoo
Before sideloading from any platform, enable USB Debugging on your Karoo unit:
1. On your Karoo, go to **Settings $\rightarrow$ About $\rightarrow$ Build Number**.
2. Tap **Build Number** 7 times continuously until the on-screen prompt says `"You are now a developer!"`.
3. Go back to **Settings $\rightarrow$ Developer Options** and toggle **USB Debugging** to **ON**.

---

### 📱 2. Sideloading Instructions by Device

#### 💻 Option 1: Computer (Mac, Windows, Linux) – Terminal ADB
1. Connect your Karoo 2 or Karoo 3 to your computer using a USB-C data cable.
2. Download [app-debug.apk](https://raw.githubusercontent.com/jdhankins/Kustom/main/releases/app-debug.apk).
3. Open Terminal (Mac/Linux) or Command Prompt / PowerShell (Windows) and run:
   ```bash
   # Confirm device connection
   adb devices

   # Sideload Kustom onto Karoo
   adb install -r app-debug.apk
