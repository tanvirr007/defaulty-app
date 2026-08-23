<div align="center">

<img src="assets/update.png" alt="Defaulty Banner" width="100%" />

# Defaulty

**A sleek, privacy-focused, and modern Android utility to manage default applications and link handling.**

[![GitHub Release](https://img.shields.io/github/v/release/tanvirr007/defaulty-app?logo=github&color=6750A4)](https://github.com/tanvirr007/defaulty-app/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/tanvirr007/defaulty-app/build.yml?branch=main&logo=githubactions&logoColor=white)](https://github.com/tanvirr007/defaulty-app/actions)
[![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/12)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-success?logo=shield&logoColor=white)](#privacy--security)
[![License](https://img.shields.io/github/license/tanvirr007/defaulty-app?color=blue)](https://opensource.org/licenses/Apache-2.0)

<br/>

[Download Latest APK](https://github.com/tanvirr007/defaulty-app/releases/latest/download/Defaulty.apk) • [Features](#key-features) • [Supported Roles](#supported-roles) • [Apply Modes](#apply-modes) • [Architecture](#architecture--tech-stack) • [Getting Started](#getting-started) • [License](#license)

</div>

---

## Overview

**Defaulty** is a lightweight, modern Android utility built using **Jetpack Compose** and **Material 3 Expressive**. It provides a centralized, transparent, and frictionless interface to inspect, configure, and manage default system roles and deep-link handling associations on Android 12+ devices.

Android system settings often bury default application switches across disparate submenus. Defaulty surfaces all available roles dynamically, queries runtime role availability, and safely delegates configuration prompts directly to Android's native `RoleManager` and `DomainVerificationManager` APIs.

---

## Key Features

- **Centralized Default Management**: View and modify default apps for web browsing, calling, messaging, home launcher, assistant, and call filtering in one unified hub.
- **Dual Apply Methods**: Standard safe delegation to Android Settings alongside copy-pasteable ADB shell commands for instant power-user switching.
- **Deep Link & App Links Handling**: Inspect and configure verified domains and link-handling behavior per app using Android's Domain Verification APIs.
- **Material 3 Expressive Design**: Genuine edge-to-edge layout, dynamic theming (Material You), smooth animations, and comprehensive Light, Dark, and System theme support.
- **100% Offline & Private**: Zero internet permissions requested in the manifest (`android.permission.INTERNET` is not included). No analytics, no tracking, no third-party SDKs, and no ads.
- **Dynamic Capability Discovery**: Probes runtime capabilities using `RoleManager.isRoleAvailable()` instead of hardcoding assumptions based on OS level.
- **Zero Overhead**: Clean Architecture, unidirectional data flow (UDF), and persistent preferences powered by AndroidX DataStore.

---

## Supported Roles

Defaulty adapts dynamically to the roles exposed and supported by the device:

| Role | Description | Android Role Name |
| :--- | :--- | :--- |
| **Browser** | Web browsing and HTTP/HTTPS links | `android.app.role.BROWSER` |
| **Phone / Dialer** | Telecom calls and dialer services | `android.app.role.DIALER` |
| **SMS / Messaging** | SMS/MMS text messaging | `android.app.role.SMS` |
| **Launcher** | Default Android home screen launcher | `android.app.role.HOME` |
| **Digital Assistant** | Voice and digital assistant services | `android.app.role.ASSISTANT` |
| **Call Screening** | Caller ID and spam call filtering | `android.app.role.CALL_SCREENING` |
| **Call Redirection** | Outgoing call redirection services | `android.app.role.CALL_REDIRECTION` |
| **Notes** | Note taking and memo applications | `android.app.role.NOTES` |
| **Wallet** | Contactless payment and wallet passes | `android.app.role.WALLET` |
| **Emergency** | Emergency assistance services | `android.app.role.EMERGENCY` |

## Apply Modes

Defaulty supports two flexible modes to manage default applications:

| Mode | Setup | Experience |
| :--- | :--- | :--- |
| **Standard Mode** | Zero Setup | Opens Android System Settings page. User taps app in settings → Returns. |
| **ADB / Shizuku Mode** | Setup Wizard | Connects to on-device ADB shell service. **1-Tap "Apply" → Toast "Done" instantly (zero extra steps).** |

### How to Enable ADB / Shizuku Mode (1-Tap Apply)

Setting up Shizuku takes less than a minute and requires no PC or root on Android 11+:

1. **Install Shizuku**:
   Get Shizuku from the [Google Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub Releases](https://github.com/RikkaApps/Shizuku/releases).

2. **Start Shizuku**:
   - **On-Device via Wireless Debugging (No PC / No Root)**:
     1. Go to **Settings → Developer Options → Enable Wireless Debugging**.
     2. Open **Shizuku** and tap **Pairing** (enter the 6-digit Wi-Fi pairing code).
     3. Tap **Start** in Shizuku.
   - **Via PC (ADB over USB)**:
     ```bash
     adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
     # Alternative for newer Android versions / restricted storage:
     adb shell sh /data/user_de/0/moe.shizuku.privileged.api/start.sh
     ```
   - **Via Root**:
     Open Shizuku and tap **Start** with Superuser / Magisk / KernelSU permission.

3. **Authorize Defaulty**:
   Open Defaulty → When prompted or in any default details screen, tap **Enable 1-Tap Apply** to allow Shizuku access.

4. **Enjoy Instant 1-Tap Defaults**:
   Select your preferred app in Defaulty and tap **Apply default** — Android sets it as default instantly in the background with a confirmation toast **"Done"**!

---

## Privacy & Security

> [!NOTE]
> **Zero Network Permissions**
> Defaulty does not declare or request network access (`android.permission.INTERNET`). The app cannot connect to the internet, transmit device telemetry, or store user information outside your device.

- **System-Handled Authorization**: Defaulty never overrides Android security controls. Any role change request triggers Android's official system confirmation dialogs.
- **Local Data Only**: App preferences (theme selection, onboarding state) are stored purely locally via **AndroidX DataStore**.
- **Open Source**: Complete transparency with all logic inspection-ready.

---

## Architecture & Tech Stack

Defaulty follows modern Android development best practices and Clean Architecture principles:

```
app/src/main/java/app/defaulty/
├── data/
│   ├── preferences/       # User preferences (Theme, Onboarding) via DataStore
│   ├── repository/        # DefaultAppRepository & LinkHandlingRepository
│   └── system/            # RoleManager, PackageManager, DomainVerificationManager wrappers
├── domain/
│   ├── model/             # Domain entities (SupportedRole, DefaultAppInfo, LinkHandlingAppInfo)
│   └── usecase/           # Business logic & role discovery use cases
├── navigation/            # Compose Navigation graph & screen destinations
├── theme/                 # Material 3 Color Schemes, Typography, Shapes & Dynamic Theming
└── ui/
    ├── components/        # Reusable UI widgets & expressive components
    ├── details/           # Role details & change default screen
    ├── home/              # Main dashboard with primary & secondary roles
    ├── links/             # Domain verification & App Links screen
    ├── onboarding/        # First-time user onboarding flow
    └── settings/          # Theme settings, privacy & about dialogs
```

### Core Technologies
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material Design 3](https://m3.material.io/)
- **Architecture**: MVVM + Clean Architecture + Repository Pattern
- **Async & Reactive**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Navigation**: AndroidX Navigation Compose (`2.9.x`)
- **Storage**: AndroidX DataStore Preferences (`1.2.x`)
- **Splash Screen**: AndroidX Core SplashScreen API (`1.2.x`)
- **Target Platform**: Android 12.0+ (API Level 31 – 36, Compile SDK 37)

---

## Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2+ (or newer)
- JDK 17 or JDK 21
- Android SDK 35+ / API 31+ device or emulator

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/tanvirr007/defaulty-app.git
cd defaulty-app

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install onto connected device
./gradlew installDebug
```

---

## Releases & CI/CD Pipeline

Every push to `main` triggers a complete GitHub Actions workflow (`.github/workflows/build.yml`):

1. **Automated Semantic Versioning**: Calculates next release tag based on git history and Gradle metadata.
2. **Release Build & ProGuard Optimization**: Builds and shrinks the release APK using R8/ProGuard.
3. **Automated GitHub Releases**: Packages versioned APKs (`Defaulty-vX.Y.Z.apk`) and a permanent direct download link (`Defaulty.apk`).
4. **Live Telegram Build Monitor**: Interactive build tracker and release notifications powered by `.github/scripts/bot.py`.

---

## License

```text
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Copyright (c) 2026 Tanvir Hasan (tanvirr007)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

This project is licensed under the terms of the **Apache License 2.0**. You are free to use, modify, and distribute this software under the terms outlined above.

---

<div align="center">
Developed and maintained by <a href="https://github.com/tanvirr007">Tanvir Hasan</a>
</div>
