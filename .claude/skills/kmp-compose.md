---
name: kmp-compose
description: Compose Multiplatform UI patterns for SportChronoClock — expect/actual composables, platform wrapping, orientation layout, and state management rules
type: workflow
---

# Compose Multiplatform UI Patterns

## Decision Tree: Where Does New UI Live?

```
Does new UI touch a native SDK or system API?
├── YES → expect fun in commonMain, actuals in androidMain + iosMain
│         Examples: maps, permissions, screen-on, camera
└── NO  → put directly in commonMain/ui/
          Examples: gauges, stat cards, layout adjustments
```

## Adding a New expect Composable

**commonMain** (`com.sportchronoclock.ui/YourView.kt`):
```kotlin
expect fun YourView(param: String, modifier: Modifier = Modifier)
```

**androidMain** (`com.sportchronoclock.ui/YourView.android.kt`):
```kotlin
actual fun YourView(param: String, modifier: Modifier) {
    AndroidView(
        factory = { context -> YourAndroidView(context).apply { /* init */ } },
        update = { view -> view.setSomething(param) },
        modifier = modifier
    )
}
```

**iosMain** (`com.sportchronoclock.ui/YourView.ios.kt`):
```kotlin
actual fun YourView(param: String, modifier: Modifier) {
    UIKitView(
        factory = { YourUIKitView() },
        update = { view -> view.something = param },
        modifier = modifier
    )
}
```

## Platform Map Patterns

**Android — MapLibre via AndroidView** (OpenFreeMap, no API key):
```kotlin
AndroidView(
    factory = { context ->
        MapView(context).apply { getMapAsync { map -> /* configure */ } }
    },
    modifier = modifier
)
```

**iOS — MKMapView via UIKitView** (MapKit, no API key):
```kotlin
UIKitView(
    factory = { MKMapView() },
    update = { mapView -> /* update camera, annotations */ },
    modifier = modifier
)
```

## DashboardScreen Orientation Layout

Do not change the outer `BoxWithConstraints` structure. Portrait vs. landscape is determined by `maxWidth < maxHeight`.

```kotlin
BoxWithConstraints(modifier) {
    if (maxWidth < maxHeight) {
        // Portrait
        Column {
            SpeedometerGauge(modifier = Modifier.fillMaxWidth().weight(0.4f), ...)
            MapView(modifier = Modifier.fillMaxWidth().weight(0.6f), ...)
        }
    } else {
        // Landscape
        Row {
            SpeedometerGauge(modifier = Modifier.fillMaxHeight().weight(0.4f), ...)
            MapView(modifier = Modifier.fillMaxHeight().weight(0.6f), ...)
        }
    }
}
```

## State Management Rule

```
ViewModel StateFlow  →  collectAsStateWithLifecycle()  →  composable params
```

```kotlin
// In screen composable
val speed by viewModel.speedKmh.collectAsStateWithLifecycle()
SpeedometerGauge(speedKmh = speed)

// NOT this — never pass ViewModel into a leaf composable
SpeedometerGauge(viewModel = viewModel)
```

Use `remember { mutableStateOf() }` only for ephemeral local UI state (e.g., dropdown open/closed). Data that survives recomposition or belongs to the domain lives in `MainViewModel`.

## Key Files

| File | Location |
|---|---|
| `DashboardScreen.kt` | `commonMain/com/sportchronoclock/ui/` |
| `SpeedometerGauge.kt` | `commonMain/com/sportchronoclock/ui/` |
| `MapView.kt` (expect) | `commonMain/com/sportchronoclock/ui/` |
| `MapView.android.kt` | `androidMain/com/sportchronoclock/ui/` |
| `MapView.ios.kt` | `iosMain/com/sportchronoclock/ui/` |
