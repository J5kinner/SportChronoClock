# BMW Speedometer Redesign + Map Pin & Directions

**Date:** 2026-04-25  
**Status:** Approved

---

## Overview

Two features:

1. **Speedometer redesign** — replace the existing neon-yellow canvas arc with a futuristic BMW i-series thin arc ring, tick marks, BMW M badge, and demo controls.
2. **Map pin & directions** — long-press to drop a pin anywhere on the map, tap it to draw an in-app route, search bar moves to bottom and hides when navigating.

---

## 1. Speedometer Redesign

### Visual Design

- **Arc**: same geometry as current (150° start, 240° sweep), but restyled:
  - Track colour: `#1a1a2e` (dark navy)
  - Progress arc: blue gradient `#0057B8 → #00B4D8`, glowing (`feGaussianBlur` drop shadow)
  - Stroke width: 10–12dp, round caps
- **Tick marks**:
  - Minor ticks every 20° across the 240° sweep — colour `#223355`, width 1.2
  - Major ticks every 60° (at 0, 50, 100, 150, 200 km/h positions) — colour `#4488bb`, width 2
  - Speed labels at major ticks: `#4488bb`, 10sp
- **Speed value**: large centred number, colour `#00B4D8`, weight 200 (light), ~44sp, with glow
- **KM/H label**: below speed value, `#334d66`, letter-spacing 4, 11sp
- **BMW M Badge**: replaces "SPORT" text
  - Small pill shape below KM/H label
  - Letter "M" in `#0057B8`, `Arial Black` / heavy weight
  - Three-segment colour stripe beneath: blue `#0057B8` / violet `#6B2F8A` / red `#C8102E`

### Demo Controls

Two distinct behaviours:

**Auto-sweep on app open (one-shot)**
- Triggers once when the composable first enters composition
- Animates `speedKmh` from 0 → `maxSpeed` → 0 using `LaunchedEffect(Unit)`
- Uses `Animatable` with `tween` easing — total ~2s (1s up, 1s down)
- Does not repeat; real GPS speed takes over immediately after

**Hold-to-demo button**
- Labelled "HOLD TO DEMO", dark blue pill button below the gauge
- Uses `pointerInput` with `detectTapGestures(onPress = …)` to detect press/release
- While held: a coroutine ramps `demoSpeed` from 0 → `maxSpeed` at a fixed rate (~30 km/h per second) using a `while(isActive)` loop with 16ms delay
- On release: ramps `demoSpeed` back to 0 at the same rate
- While demo is active, the displayed speed uses `demoSpeed` instead of the real GPS speed
- `demoSpeed` and `isDemoActive` are `MutableState` locals in `DashboardScreen` (no ViewModel needed — purely UI state)

---

## 2. Map Pin & Directions

### Pin Placement

- **Gesture**: long-press anywhere on the map drops a pin
- **Android (MapLibre)**: `MapView` registers a `MapLongClickListener` on the `MapboxMap` instance; fires `onLongPress(lat, lng)` callback passed in from `DashboardScreen`
- **iOS (MapKit)**: `UILongPressGestureRecognizer` added to the `MKMapView`; converts point to `CLLocationCoordinate2D` and fires the same callback
- `pinLocation: Pair<Double, Double>?` added to the `MapView` expect signature — `null` means no pin
- Pin is rendered as a marker:
  - Android: `SymbolManager` or `MapboxMap.addMarker` with a red pin icon
  - iOS: `MKPointAnnotation` added to the `MKMapView`
- Tapping the pin shows a callout:
  - Android: `InfoWindow` or custom `SymbolLayer` popup with a "Get Directions" button
  - iOS: `MKAnnotationView` callout accessory button labelled "Get Directions"
- Tapping "Get Directions" fires `onDirectionsRequested()` callback → `MainViewModel` calls `searchRoute(fromCurrentLocation, pinLocation)`
- A second long-press replaces the existing pin (only one pin at a time)
- Clearing the route (`CLEAR` button) also removes the pin — `MainViewModel.clearRoute()` sets both `routePoints` and `pinLocation` to null/empty

### Routing

- Reuses the existing route system in `MainViewModel` (`routePoints` StateFlow and the routing infrastructure)
- The existing `searchRoute` takes a text query and geocodes it first. Pin-based routing bypasses geocoding — `MainViewModel` gets a new `routeToPin(lat: Double, lng: Double)` function that calls the routing API directly with coordinates
- `setPinLocation` does not trigger routing automatically; only "Get Directions" tap fires `routeToPin`
- The blue polyline rendering is unchanged

### Search Bar Repositioning

- The floating search bar moves from top overlay to **bottom of the map panel**
- Alignment: `Alignment.BottomCenter` inside the `Box` wrapping `MapView`, with 12dp padding
- **Visibility rule**: the search bar is hidden when a route is active (`routePoints.isNotEmpty()`)
  - When navigating: search bar is replaced by the existing route summary bar (distance, duration, destination), repositioned to the same bottom slot
  - `CLEAR` button in the route summary clears the route and pin, making the search bar reappear

---

## 3. Architecture Changes (Option 1 — Extend Existing Signatures)

### `MapView` expect signature additions

```kotlin
@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    pinLocation: Pair<Double, Double>? = null,          // NEW
    onLongPress: (lat: Double, lng: Double) -> Unit = {}, // NEW
    onDirectionsRequested: () -> Unit = {},              // NEW
    modifier: Modifier = Modifier
)
```

### `MainViewModel` additions

```kotlin
val pinLocation: StateFlow<Pair<Double, Double>?> // NEW
fun setPinLocation(lat: Double, lng: Double)       // NEW — also clears active route
fun clearRoute()                                   // EXISTING — also clears pinLocation
```

### `SpeedometerGauge` signature (unchanged)

```kotlin
@Composable
fun SpeedometerGauge(speedKmh: Float, maxSpeed: Float = 200f)
```

Demo state (`demoSpeed`, `isDemoActive`, `autoSweepDone`) lives as `remember` locals in `DashboardScreen`, not in the gauge itself. The gauge receives a single `Float` and draws it — no change to its contract.

### `DashboardScreen` additions

- `val demoSpeed by remember { mutableStateOf(0f) }`
- `val isDemoActive by remember { mutableStateOf(false) }`
- `LaunchedEffect(Unit)` for the auto-sweep on first composition
- `pointerInput` on the HOLD TO DEMO button for the hold gesture
- Display logic: `val displaySpeed = if (isDemoActive || demoSpeed > 0f) demoSpeed else viewModel.speed`

---

## 4. Files to Change

| File | Change |
|---|---|
| `commonMain/ui/SpeedometerGauge.kt` | Full visual redesign (arc colours, tick marks, BMW M badge) |
| `commonMain/ui/DashboardScreen.kt` | Add demo state + button; move search bar to bottom; hide when route active; wire pin callbacks |
| `commonMain/ui/MapView.kt` | Add `pinLocation`, `onLongPress`, `onDirectionsRequested` to expect signature |
| `androidMain/ui/MapView.android.kt` | Implement long-press listener, pin marker, callout with Directions button |
| `iosMain/ui/MapView.ios.kt` | Add `UILongPressGestureRecognizer`, `MKPointAnnotation`, callout accessory |
| `commonMain/MainViewModel.kt` | Add `pinLocation` StateFlow, `setPinLocation()`, update `clearRoute()` |

---

## 5. Out of Scope

- Turn-by-turn voice navigation
- Multiple simultaneous pins
- Pin persistence across app restarts
- Landscape layout changes (follows existing 40/60 Row split unchanged)
