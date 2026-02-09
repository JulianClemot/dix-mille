---
name: koin
description: Koin dependency injection framework for Kotlin. Use for DI setup, module definitions, ViewModel injection, scope management, and Compose Multiplatform DI patterns.
---

# Koin Skill

Comprehensive assistance with Koin dependency injection in Kotlin Multiplatform projects.

## Quick Reference

### Core DSL Patterns

```kotlin
// Module definition
val myModule = module {
    singleOf(::MyRepository)          // Singleton
    factoryOf(::MyUseCase)            // New instance each time
    viewModelOf(::MyViewModel)        // ViewModel lifecycle
    single<Service> { ServiceImpl() } // Interface binding
}

// Dependency resolution
single { Controller(get()) }  // Resolve via get()

// Named qualifiers
single<Service>(named("default")) { ServiceImpl1() }
single<Service>(named("test")) { ServiceImpl2() }
```

### Annotation-Based Definitions

```kotlin
@Single            // Singleton definition
@Factory           // Factory definition (new instance each time)
@KoinViewModel     // ViewModel definition (Android/KMP/CMP)
@Scoped            // Scoped to a specific scope
@Named("qualifier") // Named qualifier
@Module            // Module declaration
@ComponentScan     // Auto-discover annotated definitions
```

### Common Annotations

| Annotation | Purpose | Scope |
|-----------|---------|-------|
| `@Single` | Singleton definition | App-wide |
| `@Factory` | New instance per request | Per-request |
| `@KoinViewModel` | ViewModel definition | Android/KMP/CMP |
| `@Scoped` | Scoped instance | Within scope |
| `@Named` | String/type qualifier | Disambiguation |
| `@Module` | Module declaration | Group definitions |
| `@ComponentScan` | Package scanning | Auto-discovery |

### Module Setup (DSL approach used in this project)

```kotlin
// dataModule
val dataModule = module {
    single { LocalStorage() }
    single<GameRepository> { GameRepositoryImpl(get()) }
}

// domainModule
val domainModule = module {
    factory { CreateGameUseCase(get()) }
    factory { AddScoreEntryUseCase(get()) }
    factory { CommitTurnUseCase(get()) }
}

// presentationModule
val presentationModule = module {
    viewModelOf(::GameSetupViewModel)
    viewModelOf(::ScoreSheetViewModel)
}
```

### Starting Koin

```kotlin
// Android
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(dataModule, domainModule, presentationModule, platformModule)
        }
    }
}

// In Compose
val myPresenter: MyPresenter by inject()
// Or in Composable
val viewModel: MyViewModel = koinViewModel()
```

### Interface Binding

```kotlin
// Explicit type binding
single<Service> { ServiceImpl() }

// Additional type binding
single { ServiceImpl() } bind Service::class

// Named binding
single<Service>(named("default")) { ServiceImpl1() }
single<Service>(named("test")) { ServiceImpl2() }
```

### Injection Parameters

```kotlin
// Definition with parameters
single { (view: View) -> Presenter(view) }

// Resolution with parameters
val presenter: Presenter by inject { parametersOf(view) }
```

### Scope Management

```kotlin
// Define scoped instances
scope<MyScope> {
    scoped { MyScopedService() }
}

// ViewModelScope
@ViewModelScope
class MyScopedComponent(val dep: MyDependency)
```

## Key Concepts

- **DI over Service Locator**: Koin encourages constructor injection where dependencies are passed as constructor parameters
- **Module organization**: Group related definitions in modules for maintainability
- **Platform modules**: Use expect/actual for platform-specific DI bindings
- **Singletons for stateless services**: Use `single` for repositories, use cases
- **Factory for stateful objects**: Use `factory` for objects that need fresh instances
- **ViewModel via `viewModelOf`**: Proper lifecycle management in Compose

## Reference Files

For detailed Koin documentation, see `references/getting_started.md`.
