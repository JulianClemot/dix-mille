# Dix Mille

[![CI](https://github.com/JulianClemot/dix-mille/actions/workflows/ci.yml/badge.svg)](https://github.com/JulianClemot/dix-mille/actions/workflows/ci.yml)

A Kotlin Multiplatform score sheet app for the French dice game "Dix Mille" (10,000). Players roll physical dice and use the app to track scores with strict rule enforcement. Targets Android and iOS using Compose Multiplatform with Material 3.

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **JDK** | 17+ | Kotlin compilation, Gradle |
| **Android Studio** | 2024.1+ | Android development (optional for CLI builds) |
| **Xcode** | 16.2+ | iOS development (macOS only) |
| **Android SDK** | Platform 36 | Android build target |

The project uses Gradle Wrapper (`./gradlew`), so no separate Gradle installation is needed.

## Quick Start

### Android

Build and install on a connected device or emulator:

```bash
./gradlew :androidApp:installDebug
```

Or build the APK only:

```bash
./gradlew :androidApp:assembleDebug
```

The APK is output to `androidApp/build/outputs/apk/debug/`. `:androidApp` is a thin launcher module; the shared UI and logic live in `:composeApp`.

### iOS

Build the Kotlin framework, then open Xcode to run on simulator or device:

```bash
# For iOS Simulator (Apple Silicon)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# For physical device
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

Then open and run from Xcode:

```bash
open iosApp/iosApp.xcodeproj
```

Select your target device/simulator in Xcode and press Run. Xcode handles framework embedding via a build phase script that calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.

> **Note:** You must set your development Team ID in `iosApp/Configuration/Config.xcconfig` to run on a physical device.

## Build Commands

| Command | Description |
|---------|-------------|
| `./gradlew :androidApp:assembleDebug` | Build Android debug APK |
| `./gradlew :androidApp:installDebug` | Build and install on Android device/emulator |
| `./gradlew :androidApp:assembleRelease` | Build Android release APK |
| `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` | Build iOS framework for simulator |
| `./gradlew :composeApp:linkDebugFrameworkIosArm64` | Build iOS framework for device |
| `./gradlew build` | Full build (all targets) |
| `./gradlew clean` | Clean all build outputs |

## Running Tests

All tests are in `composeApp/src/commonTest/`.

```bash
# Run all tests (JVM + iOS simulator)
./gradlew :composeApp:allTests

# Run a specific test class
./gradlew :composeApp:allTests --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest"
```

## Project Structure

```
DixMille/
├── androidApp/
│   └── src/main/              Thin Android launcher (MainActivity, manifest, res)
├── composeApp/
│   └── src/
│       ├── commonMain/        Shared code, organized as vertical feature slices
│       ├── commonTest/        Shared tests
│       ├── androidMain/       Android-specific (SharedPreferences, DI)
│       └── iosMain/           iOS-specific (MainViewController, NSUserDefaults, DI)
├── iosApp/
│   ├── iosApp.xcodeproj/     Xcode project
│   └── Configuration/        iOS build config (bundle ID, team)
├── gradle/
│   └── libs.versions.toml    Version catalog
├── docs/
│   └── SPEC.md               Game rules specification
└── CLAUDE.md                  Development guidelines
```

### Architecture

Feature-based Clean Architecture + DDD with MVVM presentation. Each feature under `feature/` is a vertical slice owning its own `domain/`, `data/`, and `presentation/` layers; shared primitives live in `core/`.

```
com.julian.dixmille/
  feature/
    home/                game_setup/         score_sheet/        game_end/     game_rules/
  core/
    domain/               data/               presentation/
      model/                source/             theme/
      repository/                               navigation/
      util/                                     component/
  di/
    AppModule.kt
```

`Game` is the Aggregate Root; domain-meaningful primitives are wrapped in Value Objects (`Score`, `PlayerId`, `TargetScore`, …). Platform-specific code uses Kotlin `expect`/`actual` declarations for `LocalStorage` (SharedPreferences on Android, NSUserDefaults on iOS) and `UuidGenerator`. Dependency injection is handled by Koin with feature-scoped modules aggregated in `AppModule.kt`. See `CLAUDE.md` for full DDD conventions.

## Key Versions

| Dependency | Version |
|------------|---------|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.12.0 |
| AGP | 9.2.0 |
| Gradle | 9.4.1 |
| Koin | 4.2.2 |
| kotlinx-serialization | 1.11.0 |
| Navigation3 | 1.1.1 |

### Android Targets

- **compileSdk**: 37
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 37

### iOS Target

- **Deployment target**: iOS 18.2
