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

## Feature-Based Architecture

Each feature is a **vertical slice** that owns every layer it needs — domain, data, and presentation — as if it were an independent Gradle module. Features must not import from each other. Shared primitives live in `core/`.

```
com.julian.dixmille/
  feature/
    score_sheet/                  # Main gameplay screen (example feature)
      domain/
        model/                    # Feature-specific models (e.g. ScoreSheetSummary)
        usecase/                  # AddScoreEntryUseCase, CommitTurnUseCase, …
        repository/               # ScoreRepository interface (if needed)
      data/
        repository/               # ScoreRepositoryImpl
      presentation/
        screen/ScoreSheetScreen.kt
        viewmodel/ScoreSheetViewModel.kt
        model/ScoreSheetUiState.kt
        model/ScoreSheetEvent.kt
        component/                # Composables used only by this feature
          PlayerScoreCard.kt
          PresetScoreButtons.kt
      di/ScoreSheetModule.kt      # Koin module for this feature only
    home/
      domain/usecase/…
      presentation/screen/…
      di/HomeModule.kt
    game_setup/
      domain/usecase/…
      data/repository/…
      presentation/screen/…
      di/GameSetupModule.kt
    game_end/
      …
    game_rules/
      …
  core/
    domain/
      model/                      # Shared primitives: Game, Player, Turn, ScoreEntry
      repository/                 # Shared repository interfaces
      util/                       # UuidGenerator (expect/actual)
    data/
      source/                     # LocalStorage (expect/actual)
    presentation/
      theme/                      # DixMilleTheme, Color
      navigation/                 # Navigator.kt, NavigationEvent
      component/                  # Composables shared by 2+ features
  di/
    AppModule.kt                  # Aggregates all feature + core Koin modules
```

### Rules

- **No cross-feature imports.** `feature/score_sheet` must never import from `feature/game_setup`. Shared logic belongs in `core/`.
- **Feature-local layers are optional.** If a feature needs no custom repository, omit the `data/` folder entirely.
- **Shared models stay in `core/domain/model/`.** Domain primitives (`Game`, `Player`, `Turn`, `ScoreEntry`) are used by multiple features and live in core.
- **Each feature registers its own Koin module** in `di/FeatureNameModule.kt`, aggregated in `AppModule.kt`.
- **Components follow the same rule**: a composable goes in `feature/<name>/presentation/component/` until a second feature needs it, then it moves to `core/presentation/component/`.

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

## Feature Development Workflow

New features follow a strict BDD + TDD pipeline using dedicated slash commands:

```
/new-feature      → Collaborative spec refinement → writes Gherkin to docs/SPEC.md
/plan-increments  → Break spec into smallest independent increments
/design-tests     → Design exhaustive test conditions for current increment
/tdd-step         → Red-green-refactor cycle (repeat per increment)
/feature-review   → Integration tests, E2E tests, then /commit on approval
```

**Repeat `/design-tests` + `/tdd-step` for each increment before running `/feature-review`.**

BDD format: strict Gherkin (Feature / Scenario / Given / When / Then).
Tests: written in `commonTest` before implementation. Named `should_<behavior>_when_<condition>`.

## Development Status

Phases 1-7 complete (domain, data, presentation, components). Phases 8-11 (turn history, UI redesign, dark theme, winner animations) are in progress. See `docs/SPEC.md` for detailed phase tracking.
