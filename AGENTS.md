# AGENTS.md

> Guidelines, architecture principles, and operational rules for AI coding agents working on the **Defaulty** repository.

---

## Repository Overview

**Defaulty** (`app.default.tanvir.info` / namespace `app.defaulty`) is an Android utility application built using Kotlin, Jetpack Compose, and Material 3. It allows users to view, manage, and configure Android default system applications (Browser, Phone, SMS, Launcher, Assistant, Call Screening, Call Redirection) and deep link domain verification handling on Android 12+ (API 31+).

- **Min SDK:** `31` (Android 12.0)
- **Target SDK:** `36` (Android 16 / 15)
- **Compile SDK:** `37`
- **JDK Target:** `JavaVersion.VERSION_17` (Build runner uses JDK 21)
- **Application ID:** `app.default.tanvir.info`
- **Namespace:** `app.defaulty`
- **Author:** Tanvir Hasan (`tanvirr007`)

---

## Architecture & Codebase Structure

The project follows **Clean Architecture** combined with **MVVM** and **Unidirectional Data Flow (UDF)**:

```
app/src/main/java/app/defaulty/
├── DefaultyApp.kt          # Application class holding singleton dependencies (UserPreferences)
├── MainActivity.kt         # Edge-to-edge entry point hosting DefaultyNavGraph
├── data/
│   ├── preferences/        # Local settings (ThemeMode, Onboarding state) via DataStore
│   ├── repository/         # Data layer repositories (DefaultAppRepository, LinkHandlingRepository)
│   └── system/             # Android OS API wrappers (RoleManager, PackageManager, DomainVerificationManager, ShizukuManager)
├── domain/
│   ├── model/              # Domain models (SupportedRole, DefaultAppInfo, LinkHandlingAppInfo)
│   └── usecase/            # Pure business logic and role querying use cases
├── navigation/             # Navigation destinations (Screen sealed class) & NavHost graph
├── theme/                  # Material 3 Color, Type, Shape & Theme providers
└── ui/
    ├── components/         # Shared Compose components (AppIcon, DefaultAppRow, CandidateAppCard, AdbCommandsDialog)
    ├── details/            # Default role details, candidate selector & dual-mode apply logic
    ├── home/               # Primary dashboard listing active & available default roles
    ├── links/              # Domain verification / App Links management screen
    ├── onboarding/         # Onboarding setup wizard (Apply mode & Theme picker)
    ├── others/             # Categorized view for Media, secondary system roles & deep link hub
    └── settings/           # Settings hub & dedicated full screens (HowItWorksScreen, ApplyModesScreen, PrivacyScreen)
```

---

## Core Engineering Rules & Constraints

When developing or modifying code in this repository, agents must adhere to the following rules:

### 1. 100% Offline & Privacy First (STRICT)
- **NEVER** add `android.permission.INTERNET` to `AndroidManifest.xml`.
- **NEVER** introduce remote analytics, tracking, telemetry, ad networks, or external HTTP libraries.
- All application data is read dynamically from local Android framework APIs and stored locally via `androidx.datastore:datastore-preferences`.
- Shizuku integration uses local Android Binder IPC over on-device ADB shell service (`dev.rikka.shizuku:api`) and requires zero network access.

### 2. Dual Apply Modes Architecture
Defaulty supports two distinct apply mechanisms:
1. **Standard Mode (Zero Setup / Default):**
   - Dispatches system intents (`RoleManager.createRequestRoleIntent()` or `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS`).
   - Delegates safely to Android's built-in Settings confirmation UI.
2. **ADB / Shizuku Mode (1-Tap Apply):**
   - Handled via `ShizukuManager` (`data/system/ShizukuManager.kt`).
   - Executes privileged ADB shell commands (`cmd role add-role-holder <roleName> <packageName> 0` and `cmd package set-home-activity`) in the background under UID 2000 (Shell).
   - Confirms instantly with a "Done" toast without navigating away from the app.
   - Always probe `ShizukuManager.hasShizukuPermission()` before attempting shell execution; gracefully fallback to Standard Mode if Shizuku is unavailable.

### 3. Android Role & System Management Conventions
- **Single Source of Truth:** `SupportedRole` enum (`app/src/main/java/app/defaulty/domain/model/SupportedRole.kt`) is the single source of truth for all supported Android system roles.
- **Dynamic Runtime Probing:** Never assume a role exists based on OS version alone. Always probe `RoleManager.isRoleAvailable(roleName)` before surfacing it in the UI.
- **Adding New Roles:** When adding support for a new Android role:
  1. Add an entry to `SupportedRole` with role string, label, icon, primary flag, description, and ADB command generator.
  2. The capability layer, UI lists, and ADB dialogs will automatically discover and display it without requiring UI rewrites.

### 4. UI & Jetpack Compose Standards
- **Material 3 Expressive:** Always use Material 3 components (`androidx.compose.material3.*`).
- **Edge-to-Edge:** Maintain genuine edge-to-edge support with `enableEdgeToEdge()` in `MainActivity.kt` and proper insets handling (`Scaffold`, `WindowInsets`, `statusBarsPadding`, `navigationBarsPadding`).
- **State Hoisting:** Keep composables stateless where possible. Screen-level composables collect `StateFlow` from ViewModels using `collectAsStateWithLifecycle()`.
- **Localization & Strings:** Never hardcode user-facing strings in Kotlin files. All strings must be defined in `app/src/main/res/values/strings.xml`.
- **Theme Support:** Ensure all UI elements adapt seamlessly to **Light**, **Dark**, and **Dynamic Color (System)** themes.

### 5. Domain Verification / Link Handling
- Domain verification features use `DomainVerificationManager` (introduced in API 31).
- Use `DomainVerificationUserState` to query domain selection status and link verification states.

---

## Build, Test & CI/CD Workflow

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (Minified + ProGuard optimized)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Print current version name
./gradlew -q printVersionName
```

### GitHub Actions CI/CD Pipeline (`.github/workflows/build.yml`)
- Triggered on push to `main` and `workflow_dispatch`.
- Automatically calculates semantic version names (`vX.Y.Z`).
- Uses `.github/scripts/bot.py` for live Telegram build progress reporting and automated release distribution.
- Signs APK using keystore secrets and generates both versioned and permanent release assets.

---

## Agent Operational Instructions

- **File Modifications:** Prefer editing existing files with minimal diffs and clean formatting.
- **ProGuard Rules:** When adding new libraries, update `app/proguard-rules.pro` if reflection, serialization, or data models are affected by R8 minification.
- **Verification:** Always verify that Gradle builds and tests pass cleanly without breaking existing navigation or theme contracts.
