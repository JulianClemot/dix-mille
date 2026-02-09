---
name: kmp-testing
description: Write comprehensive tests for Kotlin Multiplatform projects with proper test organization and coverage. Use when writing tests, adding coverage, testing edge cases, or verifying state transitions.
---

## What I do

I help you write effective tests in Kotlin Multiplatform by:

- Creating unit tests for business logic in commonTest
- Testing ViewModels and state management
- Writing Use Case tests with faked dependencies
- Testing Repository implementations
- Following AAA pattern (Arrange-Act-Assert)
- Using descriptive test names that explain behavior

## Test Organization

```
composeApp/src/
├── commonTest/kotlin/com/julian/dixmille/
│   ├── domain/
│   │   ├── usecase/     # Use case tests
│   │   └── model/       # Domain model tests
│   ├── data/
│   │   └── repository/  # Repository tests
│   └── presentation/
│       └── viewmodel/   # ViewModel tests
├── androidTest/         # Android instrumentation tests
└── iosTest/             # iOS-specific tests
```

## Test Naming Convention

Use descriptive names: **what_when_then**

```kotlin
class GameViewModelTest {
    @Test
    fun loadGame_whenGameExists_shouldUpdateStateWithGame()

    @Test
    fun loadGame_whenGameNotFound_shouldUpdateStateWithError()

    @Test
    fun rollDice_whenGameActive_shouldUpdateDiceValues()

    @Test
    fun rollDice_whenGameFinished_shouldNotUpdateDice()
}
```

## Testing Patterns

### Use Case Testing

```kotlin
class GetPlayerScoreUseCaseTest {

    private lateinit var repository: FakeGameRepository
    private lateinit var useCase: GetPlayerScoreUseCase

    @BeforeTest
    fun setup() {
        repository = FakeGameRepository()
        useCase = GetPlayerScoreUseCase(repository)
    }

    @Test
    fun invoke_whenPlayerExists_shouldReturnScore() = runTest {
        // Arrange
        val playerId = "player1"
        val expectedScore = 1500
        repository.setPlayerScore(playerId, expectedScore)

        // Act
        val result = useCase(playerId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedScore, result.getOrNull())
    }

    @Test
    fun invoke_whenPlayerNotFound_shouldReturnFailure() = runTest {
        // Act
        val result = useCase("nonexistent")

        // Assert
        assertTrue(result.isFailure)
    }
}
```

### ViewModel Testing

```kotlin
class GameViewModelTest {

    private lateinit var getGameUseCase: FakeGetGameUseCase
    private lateinit var viewModel: GameViewModel

    @BeforeTest
    fun setup() {
        getGameUseCase = FakeGetGameUseCase()
        viewModel = GameViewModel(getGameUseCase)
    }

    @Test
    fun loadGame_whenSuccess_shouldUpdateStateWithGame() = runTest {
        // Arrange
        val game = Game(id = "game1", players = emptyList())
        getGameUseCase.setResult(Result.success(game))

        // Act
        viewModel.loadGame("game1")
        advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(game.id, state.game?.id)
        assertNull(state.error)
    }
}
```

### Repository Testing

```kotlin
class GameRepositoryImplTest {

    private lateinit var localDataSource: FakeLocalGameDataSource
    private lateinit var repository: GameRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = FakeLocalGameDataSource()
        repository = GameRepositoryImpl(localDataSource)
    }

    @Test
    fun saveGame_shouldStoreGameInDataSource() = runTest {
        val game = Game(id = "game1", players = listOf(Player(id = "p1", name = "Alice")))

        val result = repository.saveGame(game)

        assertTrue(result.isSuccess)
        assertTrue(localDataSource.hasGame(game.id))
    }
}
```

## Fake Implementations

Create fakes for dependencies (better than mocks for behavior verification):

```kotlin
class FakeGameRepository : GameRepository {

    private val games = mutableMapOf<String, Game>()
    private var shouldFail = false

    fun setShouldFail(fail: Boolean) { shouldFail = fail }

    override suspend fun saveGame(game: Game): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception("Save failed"))
        } else {
            games[game.id] = game
            Result.success(Unit)
        }
    }

    override suspend fun getGame(gameId: String): Result<Game> {
        return if (shouldFail) {
            Result.failure(Exception("Get failed"))
        } else {
            games[gameId]?.let { Result.success(it) }
                ?: Result.failure(Exception("Game not found"))
        }
    }

    fun hasGame(gameId: String): Boolean = games.containsKey(gameId)
    fun clear() { games.clear(); shouldFail = false }
}
```

## Testing StateFlows

```kotlin
@Test
fun stateFlow_shouldEmitCorrectStates() = runTest {
    val states = mutableListOf<GameUiState>()
    val job = launch { viewModel.state.collect { states.add(it) } }

    viewModel.loadGame("game1")
    advanceUntilIdle()

    assertEquals(3, states.size)
    assertFalse(states[0].isLoading) // Initial
    assertTrue(states[1].isLoading)  // Loading
    assertFalse(states[2].isLoading) // Success

    job.cancel()
}
```

## Running Tests

```bash
# All common tests
./gradlew :composeApp:commonTest

# Specific test class
./gradlew :composeApp:commonTest --tests "GameViewModelTest"

# Specific test method
./gradlew :composeApp:commonTest --tests "GameViewModelTest.loadGame_whenSuccess_shouldUpdateState"

# Android tests
./gradlew :composeApp:testDebugUnitTest

# iOS tests
./gradlew :composeApp:iosSimulatorArm64Test
```

## Test Coverage Guidelines

1. **Domain layer** - 90%+ (business logic is critical)
2. **Use Cases** - 90%+ (core application logic)
3. **ViewModels** - 80%+ (state management)
4. **Repositories** - 70%+ (data operations)
5. **UI components** - Test critical paths only

## What NOT to Test

- Simple data classes with no logic
- Trivial getters/setters
- Third-party library code
- Platform-specific framework code
- Generated code

## Best Practices

1. Test behavior, not implementation
2. One assertion concept per test
3. Use descriptive test names
4. Arrange-Act-Assert pattern
5. Test edge cases and errors
6. Keep tests fast and isolated
7. Use fakes over mocks when possible
8. Test in commonTest when possible
9. Don't test private methods directly
10. Don't depend on test execution order
