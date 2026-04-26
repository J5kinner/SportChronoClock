---
name: kmp-feature
description: Step-by-step reference for adding a feature to the SportChronoClock Kotlin Multiplatform codebase
type: workflow
---

# Adding a KMP Feature

## Decision Tree

Does the feature touch platform APIs (sensors, UI widgets, permissions, OS services)?
- **No** → put everything in `commonMain`. Done.
- **Yes** → use expect/actual (see below).

## Source Set Paths

```
composeApp/src/
├── commonMain/kotlin/com/sportchronoclock/
├── androidMain/kotlin/com/sportchronoclock/
├── iosMain/kotlin/com/sportchronoclock/
└── commonTest/kotlin/com/sportchronoclock/
```

**Rule:** Package paths must mirror exactly across all source sets.

## Expect/Actual Steps

### 1. Declare in commonMain

```kotlin
// class
expect class MyThing() { fun doSomething(): String }

// composable
@Composable expect fun MyWidget(param: String)

// Koin val
expect val platformModule: Module
```

### 2. Implement in BOTH androidMain AND iosMain

**Build fails if either actual is missing.**

```kotlin
// androidMain — wrap Android Views with AndroidView {}
actual class MyThing actual constructor() { actual fun doSomething() = "android" }

@Composable actual fun MyWidget(param: String) {
    AndroidView(factory = { ctx -> /* Android View */ })
}

// iosMain — wrap UIKit widgets with UIKitView {}
actual class MyThing actual constructor() { actual fun doSomething() = "ios" }

@Composable actual fun MyWidget(param: String) {
    UIKitView(factory = { /* UIKit widget */ })
}
```

## Koin DI Registration

| What | Where |
|---|---|
| Shared (ViewModel, use case) | `commonMain/di/AppModule.kt` → `commonModule` |
| Platform deps (services, managers) | `{platform}Main/di/` → `actual val platformModule` |

```kotlin
// commonMain/di/AppModule.kt
val commonModule = module {
    viewModel { MainViewModel(get()) }
    single { MySharedUseCase(get()) }   // add here
}

// androidMain/di/  (actual val platformModule)
actual val platformModule = module {
    single { AndroidLocationProvider(androidContext()) }
    single { MyAndroidService(androidContext()) }  // add here
}
```

## ViewModel State Pattern

```kotlin
// MainViewModel.kt (commonMain)
private val _myState = MutableStateFlow<MyType>(initial)
val myState: StateFlow<MyType> = _myState.asStateFlow()

init {
    viewModelScope.launch {
        mySource.collect { raw -> _myState.value = transform(raw) }
    }
}
```

Business logic (conversions, filters) belongs in the ViewModel, not in composables.

## Checklist

- [ ] expect in `commonMain`, actual in `androidMain` + `iosMain`
- [ ] New dependency registered in `commonModule` or `platformModule`
- [ ] New ViewModel state exposed as `StateFlow<T>`
- [ ] Package path identical in all three source sets
- [ ] Tests in `commonTest` for any shared logic
