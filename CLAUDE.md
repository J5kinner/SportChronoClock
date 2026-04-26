# CLAUDE.md

ZacsSportChronoClock is a Kotlin Multiplatform (KMP) + Compose Multiplatform sports clock/speedometer app targeting Android and iOS.

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

**iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode and run from there.

## Source Set Layout

```
composeApp/src/
├── commonMain/    — Shared Kotlin logic and Compose UI
├── androidMain/   — Android-specific implementations (actual declarations)
├── iosMain/       — iOS-specific implementations (actual declarations)
└── commonTest/    — Shared tests
```

## Key Packages

- `com.sportchronoclock.location` — `LocationData` data class, `LocationProvider` interface, platform actuals
- `com.sportchronoclock.permissions` — `expect class PermissionHandler`
- `com.sportchronoclock.di` — Koin modules (`commonModule`, `expect val platformModule`)
- `com.sportchronoclock.ui` — `DashboardScreen`, `SpeedometerGauge`, `expect fun MapView`, `expect fun KeepScreenOn`, `expect fun RequestLocationPermission`
- `com.sportchronoclock.MainViewModel` — m/s → km/h conversion, low-pass filter, `StateFlow<Float>` for speed

## Expect/Actual Declarations

| commonMain `expect` | Android `actual` | iOS `actual` |
|---|---|---|
| `PermissionHandler` (class) | Uses `ContextCompat` | Uses `CLLocationManager` |
| `MapView` (composable) | MapLibre via `AndroidView` | MapKit via `UIKitView` |
| `KeepScreenOn` (composable) | `view.keepScreenOn = true` | `idleTimerDisabled = true` |
| `RequestLocationPermission` (composable) | `ActivityResultContracts` | `CLLocationManager` delegate |
| `platformModule` (val) | Koin `module { }` with Android deps | Koin `module { }` with iOS deps |

## Platform-Specific Constraints

**Android**
- Background location uses `LocationForegroundService` (foreground service) with `FusedLocationProviderClient`
- `SportChronoApplication` must be declared in `AndroidManifest.xml` as `android:name`

**iOS**
- `CLLocationManager` accuracy is `kCLLocationAccuracyBestForNavigation`; delegate is an `NSObject` subclass in Kotlin/Native
- `Info.plist` must contain `NSLocationAlwaysAndWhenInUseUsageDescription` and `NSLocationWhenInUseUsageDescription`

## Dependency Versions

See `gradle/libs.versions.toml` for version pins.

| Dependency | Version key |
|---|---|
| Koin (core, android, compose, compose-viewmodel) | `koin` |
| Google Play Services Location | `playServicesLocation` |
| MapLibre Android SDK | `maplibre` |
| kotlinx-coroutines | `coroutines` |
