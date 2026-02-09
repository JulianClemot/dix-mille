---
name: kmp-clean-architecture
description: Implement Clean Architecture patterns in Kotlin Multiplatform projects with proper layer separation. Use when creating features, refactoring architecture, designing domain models, or setting up ViewModels.
---

## What I do

I help you implement Clean Architecture in Kotlin Multiplatform projects by:

- Creating proper layer separation (Domain, Data, Presentation)
- Implementing Use Cases with single responsibility
- Designing Repository patterns with platform-agnostic interfaces
- Setting up dependency injection patterns
- Ensuring proper data flow: UI -> ViewModel -> UseCase -> Repository -> DataSource
- Applying SOLID principles throughout the architecture

## Architecture Guidelines

### Layer Structure

```
commonMain/kotlin/com/julian/dixmille/
├── domain/
│   ├── model/           # Domain entities (game state, player, etc.)
│   ├── repository/      # Repository interfaces (platform-agnostic)
│   └── usecase/         # Business logic use cases
├── data/
│   ├── repository/      # Repository implementations
│   ├── source/          # Data sources (local, remote)
│   └── mapper/          # Data <-> Domain mappers
└── presentation/
    ├── screen/          # Composable screens
    ├── viewmodel/       # ViewModels with state management
    └── model/           # UI models (different from domain models)
```

### Domain Layer Rules

1. **No framework dependencies** - Pure Kotlin only
2. **Entities are immutable** - Use `data class` with `val`
3. **Use Cases have single responsibility** - One use case, one action
4. **Repository interfaces define contracts** - Implementations in data layer

### Use Case Pattern

```kotlin
class GetPlayerScoreUseCase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(playerId: String): Result<Int> {
        return gameRepository.getPlayerScore(playerId)
    }
}
```

### Repository Pattern

```kotlin
// Domain layer - interface
interface GameRepository {
    suspend fun saveGame(game: Game): Result<Unit>
    suspend fun getGame(gameId: String): Result<Game>
}

// Data layer - implementation
class GameRepositoryImpl(
    private val localDataSource: LocalGameDataSource,
    private val gameMapper: GameMapper
) : GameRepository {
    override suspend fun saveGame(game: Game): Result<Unit> {
        return runCatching {
            val entity = gameMapper.toEntity(game)
            localDataSource.saveGame(entity)
        }
    }
}
```

### ViewModel Pattern

```kotlin
class GameViewModel(
    private val getGameUseCase: GetGameUseCase,
    private val saveGameUseCase: SaveGameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getGameUseCase(gameId)
                .onSuccess { game ->
                    _state.update {
                        it.copy(
                            game = game.toUiModel(),
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

data class GameUiState(
    val game: GameUiModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Error Handling

Use sealed classes for domain errors:

```kotlin
sealed class DomainError {
    data class ValidationError(val field: String, val message: String) : DomainError()
    data class NetworkError(val cause: Throwable?) : DomainError()
    data object NotFoundError : DomainError()
    data class UnknownError(val cause: Throwable?) : DomainError()
}

// In repositories, convert to Result
suspend fun saveGame(game: Game): Result<Unit>
```

### Dependency Direction

- **Presentation -> Domain <- Data**
- Domain layer has NO dependencies on other layers
- Data and Presentation depend on Domain
- Use dependency injection to provide implementations

## Best Practices

1. **Keep domain pure** - No Android/iOS imports in domain layer
2. **Use mappers** - Convert between Entity <-> Domain <-> UI models
3. **Inject dependencies** - Constructor injection for testability
4. **State immutability** - Use `copy()` to update states
5. **Coroutines for async** - Use suspend functions for async operations
6. **Result for errors** - Prefer `Result<T>` over exceptions in public APIs

## Common Mistakes to Avoid

- Mixing business logic in ViewModels
- ViewModels calling other ViewModels
- Domain entities with platform-specific code
- Direct database/network calls from ViewModels
- Mutable state exposure from ViewModels
- Use Cases with multiple responsibilities

## Questions to Ask

Before implementing, clarify:
1. What is the core business logic?
2. What data needs to persist?
3. What are the domain entities?
4. What validation rules exist?
5. How should errors be handled at each layer?
