---
name: kmp-clean-architecture
description: Implement Clean Architecture + DDD patterns in Kotlin Multiplatform projects. Use when creating features, refactoring architecture, designing domain models, defining Value Objects, or setting up ViewModels.
effort: high
allowed-tools: Read, Grep, Glob, Write, Edit
tags: [architecture, clean-architecture, ddd, value-objects, kotlin, multiplatform, domain]
---

## What I do

I help you implement Clean Architecture combined with DDD tactical patterns in Kotlin Multiplatform projects:

- Designing Value Objects (`@JvmInline value class`) to wrap primitives with enforced invariants
- Identifying Aggregate Roots and protecting their boundaries
- Modelling Domain Events for significant state transitions
- Placing Domain Services for cross-entity logic
- Implementing Use Cases as orchestrators (not business logic owners)
- Designing Repository patterns with platform-agnostic interfaces
- Ensuring the dependency rule: **Presentation → Domain ← Data**
- Applying ubiquitous language consistently across all layers

## Layer Structure

```
commonMain/kotlin/com/julian/dixmille/
├── core/domain/
│   ├── model/           # Aggregate roots + entities (immutable data classes)
│   │   ├── vo/          # Value Objects (@JvmInline value class)
│   │   └── event/       # Domain Events (sealed class DomainEvent)
│   ├── service/         # Domain Services (stateless, pure Kotlin)
│   ├── repository/      # Repository interfaces (platform-agnostic)
│   └── usecase/         # Use cases — orchestration only, no business logic
├── data/
│   ├── repository/      # Repository implementations
│   ├── source/          # Data sources (local, remote)
│   └── mapper/          # Data entity <-> Domain model mappers
└── presentation/
    ├── screen/          # Composable screens
    ├── viewmodel/       # ViewModels — map domain results to UI state
    └── model/           # UI models (never expose domain VOs to UI layer)
```

## DDD Tactical Patterns

### 1. Value Objects

**Every domain-meaningful primitive must be wrapped in a Value Object.** VOs enforce invariants at construction time — nothing invalid can exist in the domain.

Use `@JvmInline value class` for zero-overhead wrapping on the JVM and Native targets.

```kotlin
@JvmInline
value class Score private constructor(val value: Int) {
    override fun toString(): String = value.toString()
    companion object {
        fun of(value: Int): Score {
            require(value >= 0) { "Score cannot be negative" }
            require(value % 50 == 0) { "Score must be a multiple of 50" }
            return Score(value)
        }
        val ZERO: Score = Score(0)
    }
    operator fun plus(other: Score): Score = of(value + other.value)
    fun meetsEntryThreshold(): Boolean = value >= 500
}

@JvmInline
value class PlayerName private constructor(val value: String) {
    override fun toString(): String = value
    companion object {
        fun of(raw: String): PlayerName {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "Player name cannot be blank" }
            require(trimmed.length <= 30) { "Player name too long" }
            return PlayerName(trimmed)
        }
    }
}

@JvmInline
value class TargetScore private constructor(val value: Int) {
    override fun toString(): String = value.toString()
    companion object {
        fun of(value: Int): TargetScore {
            require(value >= 1000) { "Target score must be at least 1000" }
            return TargetScore(value)
        }
        val DEFAULT: TargetScore = TargetScore(10_000)
    }
}

@JvmInline
value class BustCount private constructor(val value: Int) {
    override fun toString(): String = value.toString()
    companion object {
        fun of(value: Int): BustCount {
            require(value in 0..3) { "Bust count must be between 0 and 3" }
            return BustCount(value)
        }
        val NONE: BustCount = BustCount(0)
    }
    fun increment(): BustCount = of(value + 1)
    fun isMaxed(): Boolean = value == 3
}
```

**Rules:**
- Always `private constructor` + `companion object { fun of(...) }` factory.
- The factory validates — throw `IllegalArgumentException` (via `require`) for contract violations.
- Never accept raw primitives (`Int`, `String`) in domain model constructors. Always the VO type.
- Always override `toString()` to return the inner value directly (`value.toString()` for numeric types, `value` for `String` types). Without this, string interpolation and UI display will render `Score(value = 500)` instead of `500`.
- VOs are equal by value, never by identity.
- Serialisation lives in the data layer only. Mappers convert VO → primitive for persistence.

### 2. Aggregate Root

The Aggregate Root is the only entry point for mutations. External code never changes child entities directly.

```kotlin
data class Game(
    val id: GameId,
    val players: List<Player>,         // entities, but mutated only via Game
    val currentPlayerIndex: Int,
    val targetScore: TargetScore,      // Value Object
    val phase: GamePhase,
    val currentTurn: Turn,
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val isInFinalRound: Boolean get() = phase == GamePhase.FINAL_ROUND

    // All state-changing operations return GameResult (new state + events)
    fun addScoreEntry(entry: ScoreEntry, validator: ScoreValidator): GameResult
    fun commitTurn(): GameResult
    fun bust(): GameResult
    fun skip(): GameResult
    fun undoLastEntry(): GameResult
}

// Carries the new aggregate state AND any emitted domain events
data class GameResult(
    val game: Game,
    val events: List<DomainEvent> = emptyList(),
)
```

**Rules:**
- Use cases call methods on the aggregate root, receive a `GameResult`, persist `game`, then dispatch `events`.
- Invariants are enforced inside aggregate methods. If an operation violates a rule, the method must not return a partial state — throw or return a `DomainError`.
- Child entities (`Player`, `Turn`, `ScoreEntry`) are owned by the aggregate and have no public setters.

### 3. Domain Events

Domain Events represent things that *happened* — immutable, past-tense facts. They decouple what happened from what reacts to it.

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

    data class FinalRoundStarted(
        val gameId: GameId,
        val leaderId: PlayerId,
    ) : DomainEvent()

    data class GameEnded(
        val gameId: GameId,
        val winnerId: PlayerId,
    ) : DomainEvent()
}
```

Events are emitted by aggregate methods (inside `GameResult`). Use cases persist state first, then dispatch events. ViewModels can react to events for navigation or animations.

### 4. Domain Services

A Domain Service holds logic that:
- Involves multiple aggregates, or
- Doesn't naturally belong to any single entity or VO.

Domain Services are **stateless**, depend only on domain types, and live in `domain/service/`.

```kotlin
// Domain Service — cross-entity rule enforcement
class ScoreValidator {
    // Does this score meet the entry threshold for a player not yet in the game?
    fun validateEntry(score: Score, player: Player): ValidationResult

    // Does the current turn constitute a bust?
    fun isBust(turn: Turn): Boolean

    // Is the game now in final round given the current player's total?
    fun shouldStartFinalRound(player: Player, targetScore: TargetScore): Boolean
}
```

**What is NOT a Domain Service:**
- A use case (orchestrates calls, not business rules)
- A repository (persistence concern)
- A mapper (data transformation)

### 5. Entities vs Value Objects — Decision Guide

| Question | Entity | Value Object |
|----------|--------|-------------|
| Does it have a lifecycle and identity? | Yes → Entity | No → VO |
| Two instances with same data — same thing? | No (different ids) | Yes (equal) |
| Example | `Player`, `Turn`, `ScoreEntry` | `Score`, `PlayerName`, `GameId` |

### 6. Ubiquitous Language

Use domain terminology precisely throughout **all layers** — domain, data, presentation, tests, and variable names:

| Term | Meaning |
|------|---------|
| **Turn** | One player's roll session — multiple score entries before commit |
| **Entry** | A single scored combination within a turn |
| **Commit** | Voluntarily ending a turn and banking the score |
| **Bust** | Failing to score on a roll — counts toward the 3-bust penalty |
| **Skip** | Voluntarily passing without rolling — NOT a bust |
| **Enter** | A player's first successful turn meeting the 500-point threshold |
| **Final Round** | Last round triggered when any player reaches the target score |

Never use generic terms like `end`, `finish`, `save` where a game-specific term applies.

## Use Case Pattern

Use cases orchestrate — they do not own business rules. Business rules live in the aggregate or domain service.

```kotlin
class CommitTurnUseCase(
    private val gameRepository: GameRepository,
    private val scoreValidator: ScoreValidator,
) {
    suspend operator fun invoke(gameId: GameId): Result<GameResult> = runCatching {
        val game = gameRepository.getGame(gameId).getOrThrow()
        val result = game.commitTurn()          // business rule is IN the aggregate
        gameRepository.saveGame(result.game).getOrThrow()
        result                                  // caller dispatches events
    }
}
```

## Repository Pattern

```kotlin
// Domain layer — interface uses domain types only, never primitives or data entities
interface GameRepository {
    suspend fun saveGame(game: Game): Result<Unit>
    suspend fun getGame(gameId: GameId): Result<Game>
}

// Data layer — maps between persistence entities and domain types
class GameRepositoryImpl(
    private val localDataSource: LocalGameDataSource,
    private val mapper: GameMapper,
) : GameRepository {
    override suspend fun saveGame(game: Game): Result<Unit> = runCatching {
        localDataSource.save(mapper.toEntity(game))
    }

    override suspend fun getGame(gameId: GameId): Result<Game> = runCatching {
        mapper.toDomain(localDataSource.load(gameId.value))
    }
}
```

**Mappers** convert `@JvmInline value class` → primitive for serialisation and back. They are the only place that knows about both the domain model and the persistence model.

## ViewModel Pattern

ViewModels map domain results to UI state. They never contain business logic.

```kotlin
class ScoreSheetViewModel(
    private val commitTurnUseCase: CommitTurnUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScoreSheetUiState())
    val state: StateFlow<ScoreSheetUiState> = _state.asStateFlow()

    private val _events = Channel<ScoreSheetNavigationEvent>(Channel.BUFFERED)
    val events: Flow<ScoreSheetNavigationEvent> = _events.receiveAsFlow()

    fun onCommitTurn(gameId: GameId) {
        viewModelScope.launch {
            commitTurnUseCase(gameId)
                .onSuccess { result ->
                    _state.update { it.copy(game = result.game.toUiModel()) }
                    result.events.forEach { handleDomainEvent(it) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
        }
    }

    private suspend fun handleDomainEvent(event: DomainEvent) {
        when (event) {
            is DomainEvent.GameEnded -> _events.send(ScoreSheetNavigationEvent.NavigateToGameEnd)
            else -> Unit
        }
    }
}
```

## Error Handling

```kotlin
// Domain layer — sealed errors with game terminology
sealed class DomainError : Exception() {
    data class ScoreBelowEntryThreshold(val score: Score) : DomainError()
    data class InvalidScoreIncrement(val score: Score) : DomainError()
    data object NoScoringDiceOnRoll : DomainError()
    data object GameAlreadyEnded : DomainError()
}

// Repositories return Result<T> — never throw raw exceptions across layer boundaries
suspend fun getGame(gameId: GameId): Result<Game>
```

## Dependency Direction

```
Presentation ──► Domain ◄── Data
```

- Domain has **zero** dependencies on other layers, Android, or iOS.
- Data and Presentation depend on Domain.
- DI (Koin) provides implementations at the composition root.

## Implementation Workflow

Follow this order — always domain-first, never top-down:

```
1. Define Value Objects (vo/) — invariants first
        ↓
2. Design Aggregate Root + Entities (model/) — no persistence concerns
        ↓
3. Define Domain Events (event/) for significant transitions
        ↓
4. Implement Domain Service (service/) if cross-entity logic exists
        ↓
5. Repository interface (domain/repository/) — domain types only
        ↓
6. Use case (domain/usecase/) — orchestrates aggregate + repository
        ↓
7. Repository implementation (data/repository/) — mappers included
        ↓
8. DI wiring (feature di module)
        ↓
9. UiState + NavigationEvent sealed classes
        ↓
10. ViewModel — maps domain results to UI state, dispatches domain events
        ↓
11. Composable screen — depends only on UiState + user intent callbacks
```

Write tests before each step. Tests use domain types — never raw primitives.

## Common Mistakes to Avoid

- Accepting raw `Int` / `String` in domain model constructors instead of VOs
- Putting validation logic in use cases instead of VO factories or aggregate methods
- Mutating child entities outside the aggregate root
- Leaking domain VOs into UI models (map to plain display types)
- Naming domain objects with technical terms (`Manager`, `Handler`) instead of ubiquitous language
- Domain Events with mutable state or future-tense names (`CommitTurn` vs `TurnCommitted`)
- Mixing business logic in ViewModels

## Questions to Ask Before Implementing

1. What are the domain invariants? (→ defines VO validation rules)
2. Who owns this state? (→ identifies the aggregate root)
3. What happened? (→ identifies domain events)
4. Does this logic cross entity boundaries? (→ decides if a domain service is needed)
5. What is the ubiquitous language term for this concept?
