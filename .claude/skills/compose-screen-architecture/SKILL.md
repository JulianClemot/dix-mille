---
name: compose-screen-architecture
description: Screen architecture pattern for Compose Multiplatform using EntryPoint + Content separation. Use when creating new screens, structuring screen composables, or splitting ViewModel concerns from UI.
---

# Compose Multiplatform Screen Architecture

## EntryPoint + Content Pattern

Each screen should be split into two composables for clean separation of concerns:

### Pattern Structure

```kotlin
// 1. Route Definition
@Serializable
data object ScreenRoute : NavKey

// 2. EntryPoint - ViewModel & State Management
@Composable
fun ScreenEntryPoint(viewModel: ScreenViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContent(state = state, onEvent = viewModel::onEvent)
}

// 3. Content - Pure UI Composable
@Composable
fun ScreenContent(
    state: ScreenUiState,
    onEvent: (ScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // UI implementation
}
```

## Responsibilities

### EntryPoint Composable

**Should:**
- Inject ViewModel (via Koin or other DI)
- Collect state using `collectAsStateWithLifecycle()`
- Pass state and event handler to Content composable
- Handle navigation events (optional, can be in Navigator)

**Should NOT:**
- Contain UI code
- Handle business logic
- Manage local UI state

```kotlin
@Composable
fun HomeEntryPoint(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.someValue) {
        // Trigger navigation, show snackbar, etc.
    }

    HomeContent(state = state, onEvent = viewModel::onEvent)
}
```

### Content Composable

**Should:**
- Render UI based on state
- Handle user interactions via events
- Manage local UI state (form fields, animations, etc.)
- Be fully testable and previewable

**Should NOT:**
- Access ViewModel directly
- Perform business logic
- Trigger navigation (should emit events instead)

```kotlin
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var localInputValue by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Text(text = state.title)

        TextField(
            value = localInputValue,
            onValueChange = { localInputValue = it }
        )

        Button(onClick = { onEvent(HomeEvent.Submit(localInputValue)) }) {
            Text("Submit")
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }
    }
}
```

## File Organization

### Single File Per Screen (Recommended for Small Screens)

```
presentation/screen/
├── HomeScreen.kt           # Contains: HomeRoute, HomeEntryPoint, HomeContent
├── DetailScreen.kt
└── ProfileScreen.kt
```

### Feature-Based Organization (Recommended for Large Screens)

```
presentation/feature/
├── home/
│   ├── HomeRoute.kt
│   ├── HomeEntryPoint.kt
│   ├── HomeContent.kt
│   ├── HomeViewModel.kt
│   ├── HomeUiState.kt
│   └── HomeEvent.kt
```

## Integration with Navigation 3

```kotlin
@Composable
fun Navigator() {
    val backStack = rememberNavBackStack(HomeRoute)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull(); true },
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    val viewModel: HomeViewModel = koinViewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.navigationEvents.collect { event ->
                            when (event) {
                                is HomeNavigationEvent.NavigateToDetail ->
                                    backStack += DetailRoute(event.itemId)
                            }
                        }
                    }

                    HomeContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        )
    }
}
```

## When to Deviate

You may use a single composable (no split) for:
- Very simple screens (< 50 lines total)
- Screens with no ViewModel (static content)
- Dialogs or bottom sheets (usually don't need ViewModels)

## Best Practices

1. **Name consistently**: `{Screen}EntryPoint`, `{Screen}Content`
2. **Keep Content pure**: No ViewModel, no DI, no side effects
3. **Use sealed classes**: For State and Event types
4. **Collect state properly**: Use `collectAsStateWithLifecycle()` not `collectAsState()`
5. **Handle navigation in EntryPoint**: Keep Content navigation-agnostic
6. **Create previews**: Always preview Content with sample data
