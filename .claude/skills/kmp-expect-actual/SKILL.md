---
name: kmp-expect-actual
description: Implement platform-specific code in Kotlin Multiplatform using expect/actual declarations correctly. Use when accessing platform APIs, creating platform abstractions, or adding new expect/actual declarations.
---

## What I do

I help you implement platform-specific functionality in Kotlin Multiplatform by:

- Creating proper expect/actual declarations
- Minimizing platform-specific code
- Providing platform abstractions
- Following KMP best practices for cross-platform code
- Ensuring type-safe platform APIs

## Expect/Actual Basics

### Pattern Structure

```
commonMain/kotlin/
└── Platform.kt              # expect declarations

androidMain/kotlin/
└── Platform.android.kt      # actual implementations for Android

iosMain/kotlin/
└── Platform.ios.kt          # actual implementations for iOS
```

### Simple Expect/Actual

```kotlin
// commonMain/Platform.kt
expect class Platform {
    val name: String
    val version: String
}

expect fun getPlatform(): Platform

// androidMain/Platform.android.kt
import android.os.Build

actual class Platform {
    actual val name: String = "Android"
    actual val version: String = Build.VERSION.SDK_INT.toString()
}

actual fun getPlatform(): Platform = Platform()

// iosMain/Platform.ios.kt
import platform.UIKit.UIDevice

actual class Platform {
    actual val name: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = Platform()
```

## Common Use Cases

### 1. UUID Generation

```kotlin
// commonMain/util/UUID.kt
expect fun randomUUID(): String

// androidMain/util/UUID.android.kt
actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()

// iosMain/util/UUID.ios.kt
actual fun randomUUID(): String = platform.Foundation.NSUUID().UUIDString
```

### 2. Current Timestamp

```kotlin
// commonMain/util/Time.kt
expect fun currentTimeMillis(): Long

// androidMain/util/Time.android.kt
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// iosMain/util/Time.ios.kt
actual fun currentTimeMillis(): Long =
    (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
```

### 3. Local Storage

```kotlin
// commonMain/data/source/LocalStorage.kt
expect class LocalStorage {
    fun save(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun clear()
}

// androidMain/data/source/LocalStorage.android.kt
actual class LocalStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dixmille_prefs", Context.MODE_PRIVATE)

    actual fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    actual fun get(key: String): String? = prefs.getString(key, null)
    actual fun remove(key: String) { prefs.edit().remove(key).apply() }
    actual fun clear() { prefs.edit().clear().apply() }
}

// iosMain/data/source/LocalStorage.ios.kt
actual class LocalStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun save(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }
    actual fun get(key: String): String? = userDefaults.stringForKey(key)
    actual fun remove(key: String) { userDefaults.removeObjectForKey(key) }
    actual fun clear() {
        val domain = NSBundle.mainBundle.bundleIdentifier
        if (domain != null) userDefaults.removePersistentDomainForName(domain)
    }
}
```

### 4. Logging

```kotlin
// commonMain/util/Logger.kt
expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
}

// androidMain - uses android.util.Log
// iosMain - uses platform.Foundation.NSLog
```

## Advanced Patterns

### Expect Interface with Factory

```kotlin
// commonMain
expect interface Vibrator {
    fun vibrate(durationMs: Long)
    fun cancel()
}

expect fun createVibrator(): Vibrator

// Platform implementations provide actual classes
```

## Best Practices

1. **Keep expect declarations simple** - Don't create complex expect classes with many methods
2. **Use interfaces for complex types** - Interface in common, platform implementations
3. **Minimize platform code** - Keep business logic in common, only platform API in actual
4. **Use typealias when appropriate** - For types that map directly to platform types

## Common Mistakes to Avoid

- Too much logic in actual implementations (keep business logic in common code)
- Duplicating code across platforms (extract common logic to commonMain)
- Exposing platform types in expect declarations (use common types in signatures)
- Not handling platform differences gracefully (design APIs that work on all platforms)

## Questions to Ask

Before creating expect/actual:
1. Is this functionality truly platform-specific?
2. Can I use a multiplatform library instead?
3. What's the smallest platform surface area needed?
4. How will this be tested on each platform?
5. Can the API be the same across platforms?
6. Should this be in domain, data, or presentation layer?
