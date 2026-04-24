# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SportChronoClock is a **Kotlin Multiplatform (KMP)** app with a **Compose Multiplatform** shared UI targeting Android and iOS. It is a sports clock / speedometer app that tracks location, displays speed, and shows a map.

## Build Commands

```bash
# Full build
./gradlew build

# Android debug APK
./gradlew :composeApp:assembleDebug

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.sportchronoclock.YourTestClass"

# Sync Gradle (useful after dependency changes)
./gradlew --refresh-dependencies
```

**iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode and run from there. Xcode must build the shared KMP framework first via a Gradle run script build phase.

## Architecture

### Source Set Layout

```
composeApp/src/
├── commonMain/    — Shared Kotlin logic and Compose UI
├── androidMain/   — Android-specific implementations (actual declarations)
├── iosMain/       — iOS-specific implementations (actual declarations)
└── commonTest/    — Shared tests
```

All shared business logic lives in `commonMain`. Platform source sets contain `actual` implementations for `expect` declarations defined in `commonMain`.

### Key Packages

- `com.sportchronoclock.location` — `LocationData` data class, `LocationProvider` interface, and platform `actual` implementations
- `com.sportchronoclock.permissions` — `expect class PermissionHandler` for checking location permission status
- `com.sportchronoclock.di` — Koin modules (`commonModule`, `expect val platformModule`)
- `com.sportchronoclock.ui` — All Compose UI: `DashboardScreen`, `SpeedometerGauge`, `expect fun MapView`, `expect fun KeepScreenOn`, `expect fun RequestLocationPermission`
- `com.sportchronoclock.MainViewModel` — Collects from `LocationProvider`, applies m/s → km/h conversion and a low-pass filter, exposes `StateFlow<Float>` for speed

### Dependency Injection

Koin is used for DI. `commonModule` (in `di/AppModule.kt`) declares the `MainViewModel`. Each platform defines `actual val platformModule` in its `di/` package, providing `LocationProvider` and `PermissionHandler`.

- Android: Koin started in `SportChronoApplication.onCreate()` with `androidContext(this)`
- iOS: Koin started in `MainViewController.kt` inside a `remember` block

### Expect/Actual Declarations

All `expect` declarations are in `commonMain`; their `actual` counterparts mirror the package path in `androidMain`/`iosMain`:

| commonMain `expect` | Android `actual` | iOS `actual` |
|---|---|---|
| `PermissionHandler` (class) | Uses `ContextCompat` | Uses `CLLocationManager` |
| `MapView` (composable) | Google Maps Compose | MapKit via `UIKitView` |
| `KeepScreenOn` (composable) | `view.keepScreenOn = true` | `idleTimerDisabled = true` |
| `RequestLocationPermission` (composable) | `ActivityResultContracts` | `CLLocationManager` delegate |
| `platformModule` (val) | Koin `module { }` with Android deps | Koin `module { }` with iOS deps |

### Platform-Specific Notes

**Android**
- Location is tracked via `FusedLocationProviderClient` inside `LocationForegroundService` (foreground service keeps tracking alive when the app is backgrounded)
- `AndroidLocationProvider` starts/stops the service and exposes the service's `SharedFlow`
- `SportChronoApplication` must be declared in `AndroidManifest.xml` as `android:name`
- Google Maps requires an API key in `AndroidManifest.xml` meta-data (`com.google.android.geo.API_KEY`); add your key to `local.properties` as `MAPS_API_KEY=...` and wire it via `manifestPlaceholders`

**iOS**
- Location is tracked via `CLLocationManager` with `kCLLocationAccuracyBestForNavigation`; the delegate is implemented as an `NSObject` subclass in Kotlin/Native
- `Info.plist` must contain `NSLocationAlwaysAndWhenInUseUsageDescription` and `NSLocationWhenInUseUsageDescription` keys
- The map uses `MKMapView` wrapped in a Compose `UIKitView`

## Dependency Versions

Key additions beyond the Compose Multiplatform boilerplate (see `gradle/libs.versions.toml`):

| Dependency | Version key |
|---|---|
| Koin (core, android, compose, compose-viewmodel) | `koin` |
| Google Play Services Location | `playServicesLocation` |
| MapLibre Android SDK | `maplibre` |
| kotlinx-coroutines | `coroutines` |

## Layout

`DashboardScreen` uses `BoxWithConstraints` to detect orientation. Portrait: `Column` (speedometer top 40%, map bottom 60%). Landscape: `Row` (speedometer left 40%, map right 60%). The layout is shared across both platforms — no expect/actual needed.

The Android map uses MapLibre (`org.maplibre.gl:android-sdk`) via `AndroidView` with OpenFreeMap tiles (no API key). iOS uses `MKMapView` via `UIKitView` (MapKit, no API key).
