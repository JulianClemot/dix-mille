---
name: compose-material3-ui
description: Build Material 3 UIs with Compose Multiplatform following design system principles. Use when creating screens, UI components, implementing Material 3 patterns, or optimizing Compose performance.
---

## What I do

I help you create polished Material 3 interfaces in Compose Multiplatform by:

- Implementing Material 3 components correctly
- Following Material Design 3 guidelines
- Creating accessible, responsive layouts
- Managing composition best practices
- Implementing proper state hoisting
- Optimizing recomposition performance

## Material 3 Fundamentals

### Color System

Use semantic color roles, not hardcoded colors:

```kotlin
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    ) {
        // Content
    }
}
```

**Color roles:**
- `primary`, `onPrimary` - Main brand color
- `secondary`, `onSecondary` - Accents
- `tertiary`, `onTertiary` - Contrast accents
- `surface`, `onSurface` - Component backgrounds
- `background`, `onBackground` - Screen backgrounds
- `error`, `onError` - Error states

### Typography

Use type scale, not hardcoded sizes:

```kotlin
Text(
    text = "Game Title",
    style = MaterialTheme.typography.headlineLarge
)

Text(
    text = "Score: 1000",
    style = MaterialTheme.typography.bodyMedium
)
```

**Type scale:**
- `displayLarge/Medium/Small` - Largest text (hero content)
- `headlineLarge/Medium/Small` - High-emphasis headings
- `titleLarge/Medium/Small` - Medium-emphasis headings
- `bodyLarge/Medium/Small` - Body text
- `labelLarge/Medium/Small` - Buttons, tabs

### Spacing

Use consistent spacing (multiples of 4dp):

```kotlin
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    // Content
}
```

**Common spacing:** 4.dp (minimal), 8.dp (small/list items), 16.dp (medium/screen padding), 24.dp (large/sections), 32.dp (extra large)

## Component Patterns

### State Hoisting

Always hoist state to make components reusable and testable:

```kotlin
// Good - Stateless, reusable
@Composable
fun PlayerCard(
    player: Player,
    isSelected: Boolean,
    onSelectPlayer: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onSelectPlayer(player) },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Text(player.name)
    }
}
```

### Screen Pattern

Separate screen logic from UI:

```kotlin
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    GameScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
private fun GameScreenContent(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dix Mille") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent()
            state.error != null -> ErrorContent(state.error)
            else -> GameContent(
                game = state.game,
                onEvent = onEvent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
```

### List Optimization

```kotlin
@Composable
fun PlayerList(
    players: List<Player>,
    onPlayerClick: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = players,
            key = { it.id } // Important for performance
        ) { player ->
            PlayerCard(
                player = player,
                onClick = { onPlayerClick(player) }
            )
        }
    }
}
```

### Side Effects

```kotlin
@Composable
fun GameScreen(viewModel: GameViewModel) {
    // Launch once when entering composition
    LaunchedEffect(Unit) {
        viewModel.loadGame()
    }

    // React to state changes
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            // Navigate back
        }
    }

    // Cleanup when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }
}
```

## Material 3 Components

### Cards

```kotlin
Card(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Title", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Content", style = MaterialTheme.typography.bodyMedium)
    }
}
```

### Buttons

```kotlin
Button(onClick = { }) { Text("Start Game") }              // High emphasis
FilledTonalButton(onClick = { }) { Text("Continue") }     // Medium emphasis
OutlinedButton(onClick = { }) { Text("Cancel") }          // Medium emphasis
TextButton(onClick = { }) { Text("Skip") }                // Low emphasis
```

### Dialogs

```kotlin
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
```

## Performance Best Practices

1. **Use `key` in lists** - Helps Compose track items
2. **Avoid creating lambdas in composition** - Use `remember` or hoist
3. **Use `derivedStateOf`** - For computed values
4. **Stable parameters** - Mark data classes with `@Immutable` or `@Stable`
5. **Avoid unnecessary recompositions** - Use `remember` wisely

```kotlin
@Composable
fun OptimizedList(items: List<Item>) {
    val sortedItems by remember(items) {
        derivedStateOf { items.sortedBy { it.priority } }
    }

    LazyColumn {
        items(items = sortedItems, key = { it.id }) { item ->
            ItemCard(item)
        }
    }
}
```

## Accessibility

1. Provide content descriptions for icons
2. Use semantic colors (not hardcoded)
3. Support dynamic type sizing
4. Ensure touch targets are 48dp minimum
5. Test with TalkBack/VoiceOver

```kotlin
IconButton(
    onClick = { /* action */ },
    modifier = Modifier.size(48.dp) // Min touch target
) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete player" // For screen readers
    )
}
```

## Questions to Ask

Before implementing UI:
1. What is the primary user action on this screen?
2. What states can this UI be in? (loading, error, empty, success)
3. Should this component be reusable?
4. What's the data flow? (events up, state down)
5. Are there navigation requirements?
6. What's the desired layout on different screen sizes?
