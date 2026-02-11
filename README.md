# Dix Mille

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
./gradlew :composeApp:installDebug
```

Or build the APK only:

```bash
./gradlew :composeApp:assembleDebug
```

The APK is output to `composeApp/build/outputs/apk/debug/`.

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
| `./gradlew :composeApp:assembleDebug` | Build Android debug APK |
| `./gradlew :composeApp:installDebug` | Build and install on Android device/emulator |
| `./gradlew :composeApp:assembleRelease` | Build Android release APK |
| `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` | Build iOS framework for simulator |
| `./gradlew :composeApp:linkDebugFrameworkIosArm64` | Build iOS framework for device |
| `./gradlew build` | Full build (all targets) |
| `./gradlew clean` | Clean all build outputs |

## Running Tests

All tests are in `composeApp/src/commonTest/` and run on the JVM.

```bash
# Run all tests
./gradlew :composeApp:allTests

# Run common tests only
./gradlew :composeApp:commonTest

# Run a specific test class
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest"

# Run a single test method
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest.someMethod"
```

## Project Structure

```
DixMille/
├── composeApp/
│   └── src/
│       ├── commonMain/        Shared code (domain, data, presentation)
│       ├── commonTest/        Shared tests
│       ├── androidMain/       Android-specific (MainActivity, SharedPreferences, DI)
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

Three-layer Clean Architecture with MVVM:

```
presentation/          domain/              data/
  screen/                model/               repository/
  viewmodel/             usecase/             source/
  component/             validation/
  model/                 repository/ (interfaces)
  navigation/
```

Platform-specific code uses Kotlin `expect`/`actual` declarations for `LocalStorage` (SharedPreferences on Android, NSUserDefaults on iOS) and `UuidGenerator`. Dependency injection is handled by Koin with platform modules.

## Key Versions

| Dependency | Version |
|------------|---------|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.0 |
| AGP | 8.11.2 |
| Gradle | 8.14.3 |
| Koin | 4.1.1 |
| kotlinx-serialization | 1.10.0 |
| Navigation3 | 1.0.0-alpha06 |

### Android Targets

- **compileSdk**: 36 (Android 15)
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 36

### iOS Target

- **Deployment target**: iOS 18.2
