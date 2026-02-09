---
name: compose-multiplatform-navigation3
description: Type-safe navigation with Jetpack Navigation 3 for Compose Multiplatform. Use when implementing navigation, adding routes, managing back stack, or setting up adaptive layouts.
---

# Compose Multiplatform Navigation 3

This skill provides guidance for implementing type-safe navigation in Compose Multiplatform projects using Jetpack Navigation 3.

## Overview

Navigation 3 is a redesigned navigation library for Compose that works across Android, iOS, desktop, and web platforms. It introduces:

- **User-owned back stack**: You create and manage a navigation back stack
- **Low-level building blocks**: More flexibility in implementing custom navigation
- **Adaptive layout system**: Display multiple destinations simultaneously
- **Type-safe navigation**: Using Kotlin serialization for destination keys
- **EntryPoint pattern**: Separation of navigation routing from UI content

## Recommended Screen Architecture Pattern

For each screen, create two composables following this pattern:

1. **EntryPoint**: Handles ViewModel injection and state collection
2. **Content**: Pure UI composable that receives state and events

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

// Define the route
@Serializable
data object HomeRoute : NavKey

// EntryPoint - ViewModel injection and state management
@Composable
fun HomeEntryPoint(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onEvent = viewModel::onEvent)
}

// Content - Pure UI composable
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit
) {
    // UI implementation
}
```

## Dependencies Setup

### Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
multiplatform-nav3-ui = "1.0.0-alpha05"

[libraries]
jetbrains-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "multiplatform-nav3-ui" }
```

## Basic Navigation Setup

### 1. Define Navigation Keys (Routes)

All destinations must be serializable and implement `NavKey`:

```kotlin
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data class DetailRoute(val itemId: String) : NavKey

@Serializable
data class ProfileRoute(val userId: String? = null) : NavKey
```

**Important**: Use `androidx.navigation3.runtime.NavKey` not `androidx.navigation3.NavKey`

### 2. Configure Polymorphic Serialization (Required for Non-JVM)

For iOS, web, and desktop, you must provide a `SerializersModule`:

```kotlin
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class, HomeRoute.serializer())
            subclass(DetailRoute::class, DetailRoute.serializer())
            subclass(ProfileRoute::class, ProfileRoute.serializer())
        }
    }
}
```

**Important**: Every route class MUST be registered in the `SerializersModule` for non-JVM platforms.

### 3. Create the Navigation Host

```kotlin
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavDisplay
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider

@Composable
fun Navigator() {
    val backStack = rememberNavBackStack(HomeRoute)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.lastOrNull() is HomeRoute) {
                false // Exit app on root screen
            } else {
                backStack.removeLastOrNull()
                true
            }
        },
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeEntryPoint()
            }

            entry<DetailRoute> { route ->
                DetailEntryPoint(itemId = route.itemId)
            }

            entry<ProfileRoute> { route ->
                ProfileEntryPoint(userId = route.userId)
            }
        }
    )
}
```

## Navigation Patterns

### Navigate Forward

```kotlin
backStack += DetailRoute(itemId = "123")
// Or
backStack.add(DetailRoute(itemId = "123"))

// Replace current destination
backStack.removeLastOrNull()
backStack += NewRoute()
```

### Navigate Back

```kotlin
backStack.removeLastOrNull()

// Pop to specific destination
backStack.removeUntil { it is HomeRoute }
```

### Conditional Navigation

```kotlin
if (backStack.lastOrNull() !is SettingsRoute) {
    backStack.add(SettingsRoute)
}

// Clear back stack and navigate
backStack.clear()
backStack.add(LoginRoute)
```

## Best Practices

1. **Define routes in a dedicated file** or colocate with the screen
2. **Centralize SerializersModule** - Register all routes in one place
3. **Use EntryPoint pattern** - ViewModel injection in EntryPoint, not in Navigator
4. **Register all routes** - Don't forget to add every route to `SerializersModule`
5. **Test on all platforms** - Navigation behavior can differ (especially iOS vs Android)
6. **Manage back stack explicitly** - You control when and how navigation happens

## Common Issues & Solutions

### "NavKey cannot be serialized"
Ensure all route classes are annotated with `@Serializable` and registered in `SerializersModule`.

### "Back stack not preserved on iOS"
Use `rememberNavBackStack()` with proper `SavedStateConfiguration`.

### "Route parameters are null after navigation"
Ensure route data classes use proper Kotlin serialization types (String, Int, Boolean, not custom objects).

## Platform-Specific Considerations

- **iOS**: Polymorphic serialization is **required** (no reflection available)
- **Android**: Can use reflection-based serialization (optional), integrates with system back button
- **Web**: Use `navigation3-browser` library for browser history integration

## Key Differences from Navigation 2

| Feature | Navigation 2 | Navigation 3 |
|---------|-------------|--------------|
| Back stack ownership | Library-owned | User-owned |
| Type safety | String routes or safe args | Kotlin serialization |
| Flexibility | High-level APIs | Low-level building blocks |
| State management | Internal | Explicit SnapshotStateList |

**Note**: Navigation 3 is in **alpha** stage. API may change before stable release.
