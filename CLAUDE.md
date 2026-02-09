# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

DixMille is a Kotlin Multiplatform Mobile (KMM) score sheet app for the French dice game "Dix Mille" (10,000). It targets Android and iOS using Compose Multiplatform with Material 3. Players roll physical dice and use the app to track scores with strict rule enforcement.

- **Package**: `com.julian.dixmille`
- **Game rules spec**: `docs/SPEC.md`

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# iOS
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj

# All tests
./gradlew :composeApp:allTests

# Common tests only
./gradlew :composeApp:commonTest

# Single test class
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest"

# Single test method
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest.someMethod"

# Full build (all targets)
./gradlew build
```

No linter is configured. Follow Kotlin official code style (set in `gradle.properties`).

## Architecture

Three-layer Clean Architecture with MVVM:

```
presentation/          domain/              data/
  screen/                model/               repository/
  viewmodel/             usecase/             source/
  component/             validation/
  model/                 repository/ (interface)
  navigation/
  theme/
```

- **Domain**: Pure business logic. `Game`, `Player`, `Turn`, `ScoreEntry` are immutable data classes. Use cases (`CreateGameUseCase`, `AddScoreEntryUseCase`, `CommitTurnUseCase`, `BustTurnUseCase`, `SkipTurnUseCase`, `UndoLastEntryUseCase`, `UndoLastTurnUseCase`) orchestrate state transitions. `ScoreValidator` enforces game rules (500-point entry, final round, bust penalties).
- **Presentation**: One ViewModel per screen, exposing `StateFlow<UiState>` and `Channel<NavigationEvent>`. Four screens: Home, GameSetup, ScoreSheet, GameEnd.
- **Data**: `GameRepositoryImpl` persists game state as JSON via `LocalStorage` (expect/actual: SharedPreferences on Android, NSUserDefaults on iOS).

### Key Patterns

- **Expect/Actual**: Platform-specific code for `LocalStorage`, `UuidGenerator`, and DI `platformModule`. Declarations in `commonMain`, implementations in `androidMain`/`iosMain`.
- **DI**: Koin with modular setup (`dataModule`, `domainModule`, `presentationModule`, `platformModule`).
- **Navigation**: Jetbrains Navigation3 with `@Serializable` route classes and centralized routing in `Navigator.kt`.
- **State updates**: Immutable data classes with `.copy()`. Coroutine-based. Auto-save to local storage on every state change.
- **Auto-commit UX**: Adding a valid score immediately commits the turn and advances to the next player (no manual "End Turn" step).

## Game Rules (Quick Reference)

- 2-6 players, target 10,000 points (configurable)
- 500-point minimum to "enter the game" on first scoring turn
- Three consecutive busts: score reverts to value before first of those 3 busts
- Final round: when any player hits target, each other player gets exactly one more turn
- Skip (voluntary, not a bust) vs Bust (no scoring dice, counts toward 3-bust penalty)

## Code Style

- Explicit imports, no wildcards. Group: stdlib, Android/iOS, third-party, project.
- Compose resources: use full path `dixmille.composeapp.generated.resources.Res`.
- Public functions and properties must have explicit return types.
- Prefer `val` over `var`, non-null types, safe calls (`?.`) and elvis (`?:`).
- Composables: PascalCase, accept `Modifier` parameter, hoist state.
- Tests in `commonTest`. Naming: `should_doX_when_conditionY`. Arrange-Act-Assert pattern.

## Dependencies

Managed via Gradle Version Catalog (`gradle/libs.versions.toml`). Key: Kotlin 2.3.x, Compose Multiplatform 1.10.x, Koin 4.x, Navigation3, kotlinx-serialization, kotlinx-coroutines.

## Development Status

Phases 1-7 complete (domain, data, presentation, components). Phases 8-11 (turn history, UI redesign, dark theme, winner animations) are in progress. See `docs/SPEC.md` for detailed phase tracking.
