# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

DixMille is a Kotlin Multiplatform Mobile (KMM) score sheet app for the French dice game "Dix Mille" (10,000). It targets Android and iOS using Compose Multiplatform with Material 3. Players roll physical dice and use the app to track scores with strict rule enforcement.

- **Package**: `com.julian.dixmille`
- **Game rules spec**: `docs/SPEC.md`

## Build Commands

```bash
# Android (entry point is now :androidApp)
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug

# iOS (framework still comes from :composeApp shared library)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj

# All tests
./gradlew :composeApp:allTests

# Single test class (use iosSimulatorArm64Test or allTests)
./gradlew :composeApp:allTests --tests "com.julian.dixmille.domain.validation.ScoreValidatorTest"

# Full build (all targets)
./gradlew build
```

No linter is configured. Follow Kotlin official code style (set in `gradle.properties`).

## Architecture

Clean Architecture + DDD with MVVM presentation. The domain layer is structured around DDD tactical patterns: Aggregate Roots, Value Objects, Domain Events, and Domain Services.

```
presentation/          domain/              data/
  screen/                model/               repository/
  viewmodel/               aggregate/         source/
  component/               vo/
  model/                   event/
  navigation/            usecase/
  theme/                 service/
                         repository/ (interface)
```

- **Domain**: Pure business logic. `Game` is the Aggregate Root. `Player`, `Turn`, `ScoreEntry` are Entities. Primitive fields are wrapped in Value Objects (`Score`, `PlayerId`, `PlayerName`, etc.). Domain Events (`TurnCommitted`, `PlayerBusted`, …) capture significant state transitions. `ScoreValidator` is a Domain Service. Use cases orchestrate aggregate operations.
- **Presentation**: One ViewModel per screen, exposing `StateFlow<UiState>` and `Channel<NavigationEvent>`. Four screens: Home, GameSetup, ScoreSheet, GameEnd.
- **Data**: `GameRepositoryImpl` persists game state as JSON via `LocalStorage` (expect/actual: SharedPreferences on Android, NSUserDefaults on iOS).

### Key Patterns

- **Expect/Actual**: Platform-specific code for `LocalStorage`, `UuidGenerator`, and DI `platformModule`. Declarations in `commonMain`, implementations in `androidMain`/`iosMain`.
- **DI**: Koin with modular setup (`dataModule`, `domainModule`, `presentationModule`, `platformModule`).
- **Navigation**: Jetbrains Navigation3 with `@Serializable` route classes and centralized routing in `Navigator.kt`.
- **State updates**: Immutable data classes with `.copy()`. Coroutine-based. Auto-save to local storage on every state change.
- **Auto-commit UX**: Adding a valid score immediately commits the turn and advances to the next player (no manual "End Turn" step).

## Domain-Driven Design

The domain layer follows DDD tactical patterns. All business rules live in the domain — never in use cases, ViewModels, or data sources.

### Value Objects

Wrap every domain-meaningful primitive in a `@JvmInline value class`. Validate invariants in the constructor, not in use cases.

```kotlin
@JvmInline
value class Score(val value: Int) : Comparable<Score> {
    init {
        require(value >= 0) { "Score cannot be negative" }
        require(value % 50 == 0) { "Score must be a multiple of 50" }
    }
    override fun toString(): String = value.toString()
    override fun compareTo(other: Score): Int = value.compareTo(other.value)
    operator fun plus(other: Score): Score = Score(value + other.value)
    fun meetsEntryThreshold(): Boolean = value >= 500
    companion object {
        val ZERO: Score = Score(0)
    }
}

@JvmInline
value class PlayerName(val value: String) {
    init {
        require(value.isNotBlank()) { "Player name cannot be blank" }
        require(value.length <= MAX_LENGTH) { "Player name too long" }
    }
    override fun toString(): String = value
    companion object {
        const val MAX_LENGTH = 30
    }
}

@JvmInline
value class TargetScore(val value: Int) {
    init {
        require(value >= 1000) { "Target score must be at least 1000" }
    }
    override fun toString(): String = value.toString()
    companion object {
        val DEFAULT: TargetScore = TargetScore(10_000)
    }
}
```

**Value Objects in this project:** `Score`, `PlayerId`, `PlayerName`, `GameId`, `TargetScore`, `BustCount`, `EntryMinimumScore`, `TurnId`, `EntryId`.

Rules:
- Use a **public constructor** with an `init` block that enforces invariants via `require`. No private constructor, no `of()` factory.
- Named constants (`ZERO`, `NONE`, `DEFAULT`) live in a `companion object` and use the public constructor directly.
- Callers handle preprocessing (e.g. `name.trim()`) before passing values to a VO constructor. The VO validates, not transforms.
- Never accept raw `Int` / `String` in domain model constructors — always the VO type.
- Always override `toString()` to return the raw value (`value.toString()` for numbers, `value` for strings).
- VOs are equal by value, not identity.
- Serialisation/deserialisation happens in the data layer only; the domain never sees raw primitives.

### Aggregate Root

`Game` is the single Aggregate Root. All mutations must go through it. Outside code never mutates `Player`, `Turn`, or `ScoreEntry` directly.

```kotlin
data class Game(
    val id: GameId,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val targetScore: TargetScore,
    val phase: GamePhase,
    val currentTurn: Turn,
) {
    // Domain logic and invariant enforcement live here
    fun addScoreEntry(entry: ScoreEntry, validator: ScoreValidator): GameResult
    fun commitTurn(): GameResult
    fun bust(): GameResult
    fun skip(): GameResult

    val currentPlayer: Player get() = players[currentPlayerIndex]
    val isInFinalRound: Boolean get() = phase == GamePhase.FINAL_ROUND
}

// GameResult carries the new state AND emitted domain events
data class GameResult(
    val game: Game,
    val events: List<DomainEvent> = emptyList(),
)
```

Rules:
- Use cases call methods on the aggregate, get a `GameResult`, persist the new `Game`, and dispatch events.
- Never bypass the aggregate root to mutate a child entity.
- Invariants are enforced inside aggregate methods — throw `IllegalStateException` (or a `DomainError`) if violated.

### Domain Events

Domain Events capture things that *happened* in the domain. They are facts — past tense, immutable.

```kotlin
sealed class DomainEvent {
    data class TurnCommitted(
        val gameId: GameId,
        val playerId: PlayerId,
        val score: Score,
    ) : DomainEvent()

    data class PlayerBusted(
        val gameId: GameId,
        val playerId: PlayerId,
        val bustCount: BustCount,
    ) : DomainEvent()

    data class TurnSkipped(
        val gameId: GameId,
        val playerId: PlayerId,
    ) : DomainEvent()

    data class PlayerEnteredGame(
        val gameId: GameId,
        val playerId: PlayerId,
    ) : DomainEvent()

    data class FinalRoundStarted(val gameId: GameId, val leaderId: PlayerId) : DomainEvent()
    data class GameEnded(val gameId: GameId, val winnerId: PlayerId) : DomainEvent()
}
```

Events are returned from aggregate methods inside `GameResult`. Use cases dispatch them after persisting state. Presentation layer can react to events (e.g., animations, navigation) via the ViewModel.

### Domain Services

A Domain Service holds logic that involves multiple aggregates or that doesn't naturally belong to any single entity.

`ScoreValidator` is the primary Domain Service: it enforces entry threshold, bust detection, and final-round rules across the whole game state. It is stateless, lives in `domain/service/`, and takes only domain types as arguments.

```kotlin
class ScoreValidator {
    fun validate(score: Score, game: Game): ValidationResult
    fun isBust(turn: Turn): Boolean
    fun hasMetEntryThreshold(player: Player): Boolean
}
```

### Ubiquitous Language

Use game terminology precisely and consistently throughout all layers:

| Term | Meaning |
|------|---------|
| **Turn** | One player's roll session (multiple entries before commit) |
| **Entry** | A single scored combination within a turn |
| **Commit** | Voluntarily ending a turn and banking the score |
| **Bust** | Failing to score on a roll — counts toward the 3-bust penalty |
| **Skip** | Voluntarily passing without rolling — NOT a bust |
| **Enter** | A player's first successful turn meeting the 500-point threshold |
| **Final Round** | The last round triggered when a player hits the target score |

Never use generic terms like `end`, `finish`, `save` where a specific game term applies.

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
- Tests in `commonTest`. Naming: backtick-quoted sentences `` `Should do X when condition Y` ``. Arrange-Act-Assert pattern.

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
