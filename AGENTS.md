# Agent Guidelines for DixMille

This document provides comprehensive guidelines for agentic coding assistants working on the DixMille Kotlin Multiplatform project.

## Project Overview

**DixMille** is a Kotlin Multiplatform Mobile (KMM) application targeting Android and iOS using Compose Multiplatform.

- **Package**: `com.julian.dixmille`
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **Gradle**: 8.14.3

## Build Commands

### Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Build release APK
./gradlew :composeApp:assembleRelease

# Install on device/emulator
./gradlew :composeApp:installDebug

# Clean build
./gradlew clean :composeApp:assembleDebug
```

### iOS

```bash
# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Build for simulator
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Open Xcode project (run from Xcode after)
open iosApp/iosApp.xcodeproj
```

### Testing

```bash
# Run all common tests
./gradlew :composeApp:allTests

# Run common tests only
./gradlew :composeApp:commonTest

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Run iOS simulator tests
./gradlew :composeApp:iosSimulatorArm64Test

# Run a single test class
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.ComposeAppCommonTest"

# Run a single test method
./gradlew :composeApp:commonTest --tests "com.julian.dixmille.ComposeAppCommonTest.example"
```

### Linting & Verification

```bash
# Build and verify all targets
./gradlew build

# Check for dependency updates
./gradlew dependencyUpdates
```

## Code Style Guidelines

### General Principles

- Follow **Kotlin official code style** (configured in gradle.properties)
- Write **scalable, maintainable, and testable** code
- Prioritize **platform-agnostic common code** over platform-specific implementations
- Use **composition over inheritance** where appropriate
- Apply **SOLID principles** and clean architecture patterns

### File Organization

```
composeApp/src/
├── commonMain/kotlin/com/julian/dixmille/
│   ├── ui/              # UI components and screens
│   ├── domain/          # Business logic and use cases
│   ├── data/            # Data layer (repositories, models)
│   └── util/            # Utility functions and extensions
├── androidMain/kotlin/com/julian/dixmille/
│   └── platform/        # Android-specific implementations
└── iosMain/kotlin/com/julian/dixmille/
    └── platform/        # iOS-specific implementations
```

### Imports

- Group imports: stdlib → Android/iOS → third-party → project
- Remove unused imports
- Use explicit imports, avoid wildcards (`import foo.*`)
- For Compose resources: use full path `dixmille.composeapp.generated.resources.Res`

```kotlin
// Good
import androidx.compose.runtime.Composable
import androidx.compose.material3.Button
import com.julian.dixmille.domain.SomeUseCase

// Bad
import androidx.compose.material3.*
```

### Naming Conventions

- **Classes/Objects**: PascalCase (`UserRepository`, `GameEngine`)
- **Functions/Variables**: camelCase (`getUserData`, `isGameActive`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_PLAYERS`, `DEFAULT_TIMEOUT`)
- **Composables**: PascalCase (`HomeScreen`, `PlayerCard`)
- **Packages**: lowercase, no underscores (`com.julian.dixmille.domain`)
- **Files**: Match primary class name (`UserRepository.kt`)

### Type Annotations

- Prefer type inference when type is obvious from context
- Always specify return types for public functions
- Use explicit types for public properties
- Prefer `val` over `var` (immutability)

```kotlin
// Good
fun getUserName(userId: String): String { ... }
private val items = listOf("a", "b", "c")

// Bad
fun getUserName(userId: String) = repo.getName(userId) // Missing return type
var items = listOf("a", "b", "c") // Should be val
```

### Nullability

- Prefer non-null types when possible
- Use safe calls (`?.`) and elvis operator (`?:`) over explicit null checks
- Avoid `!!` operator unless you can guarantee non-null

```kotlin
// Good
val name = user?.name ?: "Unknown"

// Bad
val name = if (user != null) user.name else "Unknown"
```

### Expect/Actual Pattern

For platform-specific code, use expect/actual declarations:

```kotlin
// commonMain/Platform.kt
expect fun getPlatform(): Platform

// androidMain/Platform.android.kt
actual fun getPlatform(): Platform = AndroidPlatform()

// iosMain/Platform.ios.kt
actual fun getPlatform(): Platform = IOSPlatform()
```

### Compose Best Practices

- Keep composables **small and focused** (single responsibility)
- Use `remember` for state that survives recomposition
- Hoist state when possible (pass state down, events up)
- Use `LaunchedEffect` for side effects with lifecycle awareness
- Prefer `Modifier` parameters for styling flexibility
- Use `@Preview` annotation for Android Studio previews

```kotlin
@Composable
fun PlayerCard(
    player: Player,
    onPlayerClick: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onPlayerClick(player) },
        modifier = modifier
    ) {
        Text(player.name)
    }
}
```

### Error Handling

- Use `Result<T>` for operations that may fail
- Create sealed class hierarchies for domain errors
- Handle errors at appropriate layers
- Log errors with meaningful context

```kotlin
sealed class GameError {
    data object NetworkError : GameError()
    data class InvalidMove(val reason: String) : GameError()
}

suspend fun makeMove(move: Move): Result<GameState> = runCatching {
    // Implementation
}
```

### Testing

- Place common tests in `commonTest`
- Test business logic in isolation from platform code
- Use descriptive test names: `should_returnError_when_invalidInput`
- Follow Arrange-Act-Assert pattern
- Mock external dependencies

```kotlin
class GameEngineTest {
    @Test
    fun should_calculateScore_when_validMove() {
        // Arrange
        val engine = GameEngine()
        val move = Move(...)
        
        // Act
        val score = engine.calculateScore(move)
        
        // Assert
        assertEquals(100, score)
    }
}
```

### Documentation

- Document public APIs with KDoc
- Explain **why**, not **what** (code shows what)
- Document complex algorithms or business logic
- Keep comments up-to-date with code changes

```kotlin
/**
 * Calculates the final score for a player based on their moves.
 * 
 * Scoring follows the traditional DixMille rules where consecutive
 * moves multiply the base score by 1.5x.
 *
 * @param moves List of moves made by the player
 * @return Final calculated score
 */
fun calculateFinalScore(moves: List<Move>): Int { ... }
```

## Key Technologies & Dependencies

- **UI**: Compose Multiplatform (Material 3)
- **Lifecycle**: AndroidX Lifecycle (ViewModel, Runtime Compose)
- **Testing**: kotlin-test
- **Build**: Gradle with Kotlin DSL, Version Catalog

## Important Notes

- Minimum Android SDK: **24** (Android 7.0)
- Target Android SDK: **36**
- Java compatibility: **11**
- iOS deployment targets: **iosArm64** (device), **iosSimulatorArm64** (simulator)
- Framework name: **ComposeApp** (static framework)
- No code linting configured yet - follow Kotlin official style manually

## Common Tasks

### Adding a new dependency

Edit `gradle/libs.versions.toml`, then sync Gradle:

```toml
[versions]
ktor = "2.3.0"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
```

### Creating a new screen

1. Create composable in `commonMain/kotlin/com/julian/dixmille/ui/screens/`
2. Follow naming: `[Feature]Screen.kt`
3. Use Material 3 components
4. Test on both Android and iOS

### Adding platform-specific code

1. Define `expect` declaration in `commonMain`
2. Implement `actual` in `androidMain` and `iosMain`
3. Keep platform code minimal

## Architecture Recommendations

- **Use MVVM** with ViewModels for business logic
- **Separate concerns**: UI → ViewModel → Repository → Data Source
- **Common code first**: maximize shared code, minimize platform-specific
- **Dependency injection**: consider Koin or manual DI for now
- **Navigation**: plan for compose-navigation when adding multiple screens
- **State management**: use Compose state for UI, StateFlow for ViewModels

## Performance Guidelines

- Avoid unnecessary recompositions (use `remember`, `derivedStateOf`)
- Use `LazyColumn`/`LazyRow` for long lists
- Load images efficiently with appropriate libraries (Coil, Kamel)
- Profile both Android and iOS builds separately
- Keep iOS framework size minimal (impacts app size)

---

**Remember**: This is a **scalable, production-ready KMP project**. Write code that is maintainable, testable, and follows Kotlin Multiplatform best practices. When in doubt, prefer common code over platform-specific implementations.
