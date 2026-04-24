# BMW Speedometer Redesign + Map Pin & Directions — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the neon-yellow speedometer arc with a futuristic BMW M dial (tick marks, gradient arc, BMW M badge, demo controls) and add long-press map pins with in-app route drawing and a bottom-anchored search bar that hides during navigation.

**Architecture:** Extend the existing `expect`/`actual` `MapView` signature with three new params (`pinLocation`, `onLongPress`, `onDirectionsRequested`). Pin state and routing live in `MainViewModel`. Demo speed is local `remember` state in `DashboardScreen`. All changes follow the pattern of passing primitives into composables.

**Tech Stack:** Kotlin Multiplatform + Compose Multiplatform, Koin DI, MapLibre Android SDK 11.5.2 (Android), MapKit (iOS), Kotlinx Coroutines 1.9.0 + Compose Animation, Ktor 3.1.2 (routing), kotlin-test + coroutines-test + ktor-client-mock (tests).

---

## File Map

| File | Change |
|---|---|
| `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/SpeedometerGauge.kt` | Full visual redesign |
| `composeApp/src/commonMain/kotlin/com/sportchronoclock/navigation/DirectionsService.kt` | Add `routeToCoordinates` |
| `composeApp/src/commonMain/kotlin/com/sportchronoclock/MainViewModel.kt` | Add `pinLocation`, `setPinLocation`, `routeToPin`; update `clearRoute` |
| `gradle/libs.versions.toml` | Add `kotlinx-coroutines-test` and `ktor-client-mock` |
| `composeApp/build.gradle.kts` | Wire new test deps into `commonTest` |
| `composeApp/src/commonTest/kotlin/com/sportchronoclock/MainViewModelTest.kt` | New tests (create) |
| `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/MapView.kt` | Extend expect signature |
| `composeApp/src/androidMain/kotlin/com/sportchronoclock/ui/MapView.android.kt` | Long-press, pin marker, info-window callout |
| `composeApp/src/iosMain/kotlin/com/sportchronoclock/ui/MapView.ios.kt` | Long-press gesture, `MKPointAnnotation`, callout delegate |
| `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/DashboardScreen.kt` | Demo controls, search bar repositioned to bottom, pin callbacks |

---

## Task 1: Redesign SpeedometerGauge

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/SpeedometerGauge.kt`

- [ ] **Step 1: Replace SpeedometerGauge.kt with the BMW redesign**

```kotlin
package com.sportchronoclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ArcTrack = Color(0xFF1a1a2e)
private val ProgressStart = Color(0xFF0057B8)
private val ProgressEnd = Color(0xFF00B4D8)
private val SpeedTextColor = Color(0xFF00B4D8)
private val UnitTextColor = Color(0xFF334d66)
private val LabelColor = Color(0xFF4488bb)
private val MinorTickColor = Color(0xFF223355)
private val BadgeBackground = Color(0xFF111122)
private val BadgeBorder = Color(0xFF1a2a3a)
private val MBlue = Color(0xFF0057B8)
private val MViolet = Color(0xFF6B2F8A)
private val MRed = Color(0xFFC8102E)

@Composable
fun SpeedometerGauge(
    speedKmh: Float,
    maxSpeed: Float = 200f,
    modifier: Modifier = Modifier
) {
    val progress = (speedKmh / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 150f
    val totalSweep = 240f
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.07f
            val inset = strokeWidth / 2f + size.minDimension * 0.12f
            val arcSize = Size(size.minDimension - inset * 2, size.minDimension - inset * 2)
            val arcOffset = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )
            val arcRadius = arcSize.width / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background track
            drawArc(
                color = ArcTrack,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (progress > 0f) {
                // Glow layer
                drawArc(
                    color = ProgressStart.copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = totalSweep * progress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(ProgressStart, ProgressEnd),
                        start = Offset(arcOffset.x, arcOffset.y + arcSize.height),
                        end = Offset(arcOffset.x + arcSize.width, arcOffset.y)
                    ),
                    startAngle = startAngle,
                    sweepAngle = totalSweep * progress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Tick marks drawn outside the arc ring — 13 positions every 20°
            val tickStartR = arcRadius + strokeWidth / 2f + size.minDimension * 0.015f
            val minorLen = size.minDimension * 0.04f
            val majorLen = size.minDimension * 0.07f
            val majorIndices = setOf(0, 3, 6, 9, 12)

            for (i in 0..12) {
                val t = i * (totalSweep / 12f)
                val angleRad = ((startAngle + t) * PI / 180.0).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val isMajor = i in majorIndices
                val len = if (isMajor) majorLen else minorLen
                val color = if (isMajor) LabelColor else MinorTickColor
                val sw = if (isMajor) strokeWidth * 0.14f else strokeWidth * 0.09f
                drawLine(
                    color = color,
                    start = Offset(center.x + tickStartR * cosA, center.y + tickStartR * sinA),
                    end = Offset(
                        center.x + (tickStartR + len) * cosA,
                        center.y + (tickStartR + len) * sinA
                    ),
                    strokeWidth = sw
                )
            }

            // Speed labels at major tick positions (0, 50, 100, 150, 200)
            val labelR = tickStartR + majorLen + size.minDimension * 0.045f
            val labelValues = listOf("0", "50", "100", "150", "200")
            majorIndices.sorted().forEachIndexed { idx, majorIdx ->
                val t = majorIdx * (totalSweep / 12f)
                val angleRad = ((startAngle + t) * PI / 180.0).toFloat()
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val layout = textMeasurer.measure(
                    text = labelValues[idx],
                    style = TextStyle(color = LabelColor, fontSize = 9.sp)
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = center.x + labelR * cosA - layout.size.width / 2f,
                        y = center.y + labelR * sinA - layout.size.height / 2f
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = speedKmh.toInt().toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = SpeedTextColor
            )
            Text(
                text = "KM/H",
                fontSize = 10.sp,
                color = UnitTextColor,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            // BMW M Badge
            Box(
                modifier = Modifier
                    .background(BadgeBackground, RoundedCornerShape(4.dp))
                    .border(0.5.dp, BadgeBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "M",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MBlue,
                        letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Box(Modifier.width(7.dp).height(2.dp).background(MBlue, RoundedCornerShape(1.dp)))
                        Box(Modifier.width(7.dp).height(2.dp).background(MViolet, RoundedCornerShape(1.dp)))
                        Box(Modifier.width(7.dp).height(2.dp).background(MRed, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to confirm it compiles**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/SpeedometerGauge.kt
git commit -m "feat: BMW M arc speedometer redesign with tick marks and gradient arc"
```

---

## Task 2: Add routeToCoordinates + pinLocation to ViewModel, with tests

**Files:**
- Modify: `composeApp/gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Modify: `composeApp/src/commonMain/kotlin/com/sportchronoclock/navigation/DirectionsService.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/sportchronoclock/MainViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/sportchronoclock/MainViewModelTest.kt`

- [ ] **Step 1: Add test libraries to libs.versions.toml**

In `gradle/libs.versions.toml`, add two entries to the `[libraries]` section:

```toml
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
```

- [ ] **Step 2: Wire the new libs into commonTest**

In `composeApp/build.gradle.kts`, replace the existing `commonTest.dependencies` block:

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.ktor.client.mock)
}
```

- [ ] **Step 3: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/sportchronoclock/MainViewModelTest.kt`:

```kotlin
package com.sportchronoclock

import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private class FakeLocationProvider : LocationProvider {
    val flow = MutableSharedFlow<LocationData>()
    override val locationFlow: Flow<LocationData> = flow
    override fun startTracking() {}
    override fun stopTracking() {}
}

private fun fakeDirectionsService() = DirectionsService(
    HttpClient(MockEngine { _ -> respond("", HttpStatusCode.OK) })
)

class MainViewModelTest {

    @Test
    fun setPinLocationStoresCoordinates() = runTest {
        val vm = MainViewModel(FakeLocationProvider(), fakeDirectionsService())
        vm.setPinLocation(51.5074, -0.1278)
        assertEquals(51.5074 to -0.1278, vm.pinLocation.value)
    }

    @Test
    fun clearRouteAlsoClearsPinLocation() = runTest {
        val vm = MainViewModel(FakeLocationProvider(), fakeDirectionsService())
        vm.setPinLocation(51.5074, -0.1278)
        vm.clearRoute()
        assertNull(vm.pinLocation.value)
    }

    @Test
    fun routeToPinWithNoGpsFixSetsError() = runTest {
        val vm = MainViewModel(FakeLocationProvider(), fakeDirectionsService())
        // _locationData is null — no GPS fix yet
        vm.routeToPin(51.5074, -0.1278)
        assertTrue(vm.searchState.value is SearchState.Error)
    }
}
```

- [ ] **Step 4: Run tests — expect FAIL**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.sportchronoclock.MainViewModelTest"
```
Expected: FAIL — `setPinLocation`, `routeToPin`, `pinLocation` not defined yet.

- [ ] **Step 5: Add routeToCoordinates to DirectionsService**

In `composeApp/src/commonMain/kotlin/com/sportchronoclock/navigation/DirectionsService.kt`, add after `searchAndRoute`:

```kotlin
suspend fun routeToCoordinates(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double
): Result<RouteResult> = runCatching {
    val destinationName = "%.5f, %.5f".format(toLat, toLon)
    getRoute(fromLat, fromLon, toLat, toLon, destinationName)
        ?: error("Could not calculate a route")
}
```

- [ ] **Step 6: Replace MainViewModel.kt with new version including pin support**

```kotlin
package com.sportchronoclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.RouteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationProvider: LocationProvider,
    private val directionsService: DirectionsService
) : ViewModel() {

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()

    private val _routeResult = MutableStateFlow<RouteResult?>(null)
    val routeResult: StateFlow<RouteResult?> = _routeResult.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _pinLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val pinLocation: StateFlow<Pair<Double, Double>?> = _pinLocation.asStateFlow()

    private val alpha = 0.15f
    private var filteredSpeed = 0f

    init {
        viewModelScope.launch {
            locationProvider.locationFlow.collect { data ->
                _locationData.value = data
                val rawKmh = maxOf(0f, data.speed) * 3.6f
                filteredSpeed = alpha * rawKmh + (1f - alpha) * filteredSpeed
                _speedKmh.value = filteredSpeed
            }
        }
    }

    fun startTracking() = locationProvider.startTracking()

    fun stopTracking() = locationProvider.stopTracking()

    fun searchRoute(query: String) {
        val location = _locationData.value ?: run {
            _searchState.value = SearchState.Error("Waiting for GPS fix — try again in a moment")
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            directionsService.searchAndRoute(query, location.latitude, location.longitude)
                .onSuccess { result ->
                    _routeResult.value = result
                    _searchState.value = SearchState.Idle
                }
                .onFailure { e ->
                    _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                }
        }
    }

    fun setPinLocation(lat: Double, lng: Double) {
        _pinLocation.value = lat to lng
        _routeResult.value = null
        _searchState.value = SearchState.Idle
    }

    fun routeToPin(toLat: Double, toLng: Double) {
        val location = _locationData.value ?: run {
            _searchState.value = SearchState.Error("Waiting for GPS fix — try again in a moment")
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            directionsService.routeToCoordinates(location.latitude, location.longitude, toLat, toLng)
                .onSuccess { result ->
                    _routeResult.value = result
                    _searchState.value = SearchState.Idle
                }
                .onFailure { e ->
                    _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                }
        }
    }

    fun clearRoute() {
        _routeResult.value = null
        _pinLocation.value = null
        _searchState.value = SearchState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopTracking()
    }
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Error(val message: String) : SearchState
}
```

- [ ] **Step 7: Run tests — expect PASS**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.sportchronoclock.MainViewModelTest"
```
Expected: 3 tests PASS

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml \
        composeApp/build.gradle.kts \
        composeApp/src/commonMain/kotlin/com/sportchronoclock/navigation/DirectionsService.kt \
        composeApp/src/commonMain/kotlin/com/sportchronoclock/MainViewModel.kt \
        composeApp/src/commonTest/kotlin/com/sportchronoclock/MainViewModelTest.kt
git commit -m "feat: pin routing — add routeToCoordinates, pinLocation, routeToPin, update clearRoute"
```

---

## Task 3: Extend MapView expect signature

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/MapView.kt`

- [ ] **Step 1: Replace MapView.kt**

```kotlin
package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    pinLocation: Pair<Double, Double>? = null,
    onLongPress: (lat: Double, lng: Double) -> Unit = {},
    onDirectionsRequested: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Confirm the actuals are now broken (expected at this stage)**

```bash
./gradlew :composeApp:assembleDebug 2>&1 | grep "error:"
```
Expected: two `error:` lines — Android and iOS `actual` functions are missing the new params.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/MapView.kt
git commit -m "feat: extend MapView expect with pinLocation and long-press callbacks"
```

---

## Task 4: Android MapView — long-press, pin marker, info-window callout

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/sportchronoclock/ui/MapView.android.kt`

- [ ] **Step 1: Replace MapView.android.kt**

```kotlin
package com.sportchronoclock.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val ROUTE_SOURCE = "route-source"
private const val ROUTE_LAYER = "route-layer"

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>>,
    pinLocation: Pair<Double, Double>?,
    onLongPress: (lat: Double, lng: Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDirectionsRequested = rememberUpdatedState(onDirectionsRequested)
    val pinMarker = remember { mutableStateOf<Marker?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    // Load style + register listeners once
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(STYLE_URL))
            map.uiSettings.apply {
                isRotateGesturesEnabled = false
                isCompassEnabled = false
            }
            map.addOnMapLongClickListener { latLng ->
                currentOnLongPress.value(latLng.latitude, latLng.longitude)
                true
            }
            map.setOnInfoWindowClickListener { _ ->
                currentOnDirectionsRequested.value()
                true
            }
        }
    }

    // Animate camera to current position
    LaunchedEffect(latitude, longitude, bearing) {
        mapView.getMapAsync { map ->
            val target = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(16.0)
                .bearing(bearing.toDouble())
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(target), 800)
        }
    }

    // Draw or clear route polyline
    LaunchedEffect(routePoints) {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                style.removeLayer(ROUTE_LAYER)
                style.removeSource(ROUTE_SOURCE)
                if (routePoints.isNotEmpty()) {
                    val coords = routePoints.joinToString(",") { (lat, lon) -> "[$lon,$lat]" }
                    val geoJson = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]},"properties":{}}]}"""
                    style.addSource(GeoJsonSource(ROUTE_SOURCE, geoJson))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                            PropertyFactory.lineColor("#1E88E5"),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )
                }
            }
        }
    }

    // Add or remove the pin marker; tapping its info-window fires onDirectionsRequested
    LaunchedEffect(pinLocation) {
        mapView.getMapAsync { map ->
            pinMarker.value?.let { map.removeMarker(it) }
            pinMarker.value = null
            if (pinLocation != null) {
                val (lat, lng) = pinLocation
                pinMarker.value = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title("Get Directions")
                )
            }
        }
    }

    // Forward Android lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
```

- [ ] **Step 2: Build Android to confirm it compiles (iOS still broken)**

```bash
./gradlew :composeApp:assembleDebug 2>&1 | grep -E "BUILD|error:"
```
Expected: `error:` only from iOS actual, no Android errors. Or BUILD SUCCESSFUL if Gradle skips iOS.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/sportchronoclock/ui/MapView.android.kt
git commit -m "feat: Android MapView — long-press pin and info-window directions callout"
```

---

## Task 5: iOS MapView — long-press gesture, MKPointAnnotation, callout delegate

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/com/sportchronoclock/ui/MapView.ios.kt`

- [ ] **Step 1: Replace MapView.ios.kt**

```kotlin
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.*
import platform.UIKit.*
import platform.darwin.NSObject

private class MapDelegate(
    var onDirectionsRequested: () -> Unit = {},
    var onLongPress: (Double, Double) -> Unit = { _, _ -> }
) : NSObject(), MKMapViewDelegateProtocol {

    override fun mapView(
        mapView: MKMapView,
        rendererForOverlay: MKOverlayProtocol
    ): MKOverlayRenderer {
        if (rendererForOverlay is MKPolyline) {
            return MKPolylineRenderer(rendererForOverlay as MKPolyline).apply {
                strokeColor = UIColor(red = 0.118, green = 0.533, blue = 0.898, alpha = 1.0)
                lineWidth = 6.0
            }
        }
        return MKOverlayRenderer(rendererForOverlay)
    }

    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: MKAnnotationProtocol
    ): MKAnnotationView? {
        if (viewForAnnotation is MKUserLocation) return null
        return MKPinAnnotationView(
            annotation = viewForAnnotation,
            reuseIdentifier = "pin"
        ).apply {
            canShowCallout = true
            pinTintColor = UIColor.redColor
            rightCalloutAccessoryView = UIButton.buttonWithType(UIButtonTypeDetailDisclosure)
        }
    }

    override fun mapView(
        mapView: MKMapView,
        annotationView: MKAnnotationView,
        calloutAccessoryControlTapped: UIControl
    ) {
        onDirectionsRequested()
    }

    @ObjCAction
    fun handleLongPress(recognizer: UILongPressGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan) {
            val mapView = recognizer.view as? MKMapView ?: return
            val point = recognizer.locationInView(mapView)
            val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)
            coordinate.useContents { onLongPress(latitude, longitude) }
        }
    }
}

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>>,
    pinLocation: Pair<Double, Double>?,
    onLongPress: (lat: Double, lng: Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    modifier: Modifier
) {
    val delegate = remember { MapDelegate() }
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDirectionsRequested = rememberUpdatedState(onDirectionsRequested)

    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().also { map ->
                map.delegate = delegate
                map.showsUserLocation = true
                map.pitchEnabled = false
                map.rotateEnabled = false
                val recognizer = UILongPressGestureRecognizer(
                    target = delegate,
                    action = NSSelectorFromString("handleLongPress:")
                ).apply { minimumPressDuration = 0.5 }
                map.addGestureRecognizer(recognizer)
            }
        },
        update = { map ->
            // Keep delegate callbacks current without re-running factory
            delegate.onLongPress = { lat, lng -> currentOnLongPress.value(lat, lng) }
            delegate.onDirectionsRequested = { currentOnDirectionsRequested.value() }

            // Camera
            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
            val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                centerCoordinate = coordinate,
                fromDistance = 500.0,
                pitch = 0.0,
                heading = bearing.toDouble()
            )
            map.setCamera(camera, animated = true)

            // Route overlays
            map.overlays.filterIsInstance<MKPolyline>().forEach { map.removeOverlay(it) }
            if (routePoints.isNotEmpty()) {
                memScoped {
                    val coords = allocArray<CLLocationCoordinate2D>(routePoints.size)
                    routePoints.forEachIndexed { i, (lat, lon) ->
                        coords[i].latitude = lat
                        coords[i].longitude = lon
                    }
                    map.addOverlay(
                        MKPolyline.polylineWithCoordinates(coords, routePoints.size.toULong())
                    )
                }
            }

            // Pin annotation — remove any existing, add new one if set
            map.annotations.filterIsInstance<MKPointAnnotation>().forEach {
                map.removeAnnotation(it)
            }
            if (pinLocation != null) {
                val (lat, lng) = pinLocation
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                annotation.title = "Pin"
                map.addAnnotation(annotation)
            }
        }
    )
}
```

- [ ] **Step 2: Build the full project**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: BUILD SUCCESSFUL — both actuals now match the expect signature.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/sportchronoclock/ui/MapView.ios.kt
git commit -m "feat: iOS MapView — long-press pin, MKPointAnnotation, callout directions button"
```

---

## Task 6: DashboardScreen — demo controls, bottom search bar, pin wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/DashboardScreen.kt`

- [ ] **Step 1: Replace DashboardScreen.kt**

```kotlin
package com.sportchronoclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.SearchState
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.RouteResult
import com.sportchronoclock.permissions.PermissionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(
    permissionHandler: PermissionHandler = koinInject()
) {
    val locationProvider = koinInject<LocationProvider>()
    val directionsService = koinInject<DirectionsService>()
    val viewModel: MainViewModel = viewModel { MainViewModel(locationProvider, directionsService) }

    val speedKmh by viewModel.speedKmh.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val routeResult by viewModel.routeResult.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val pinLocation by viewModel.pinLocation.collectAsState()

    var hasPermission by remember { mutableStateOf(permissionHandler.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Demo state — lives here, not in the gauge
    val demoScope = rememberCoroutineScope()
    var demoSpeed by remember { mutableStateOf(0f) }
    var isDemoActive by remember { mutableStateOf(false) }
    var demoRampJob by remember { mutableStateOf<Job?>(null) }
    var autoSweepDone by remember { mutableStateOf(false) }
    val sweepAnimatable = remember { Animatable(0f) }

    // One-shot sweep animation on first composition
    LaunchedEffect(Unit) {
        sweepAnimatable.animateTo(200f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        sweepAnimatable.animateTo(0f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        autoSweepDone = true
    }

    val displaySpeed = when {
        isDemoActive || demoSpeed > 0f -> demoSpeed
        !autoSweepDone -> sweepAnimatable.value
        else -> speedKmh
    }

    KeepScreenOn()

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startTracking()
    }

    if (!hasPermission && !permissionDenied) {
        RequestLocationPermission(
            onGranted = { hasPermission = true },
            onDenied = { permissionDenied = true }
        )
    }

    val statusMessage = when {
        permissionDenied -> "Location permission denied.\nPlease enable it in Settings."
        !hasPermission -> "Requesting location permission…"
        else -> "Acquiring GPS signal…"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            val onDemoPress: () -> Unit = {
                demoRampJob?.cancel()
                isDemoActive = true
                demoSpeed = 0f
                demoRampJob = demoScope.launch {
                    while (demoSpeed < 200f) {
                        demoSpeed = (demoSpeed + 0.5f).coerceAtMost(200f)
                        delay(16)
                    }
                }
            }
            val onDemoRelease: () -> Unit = {
                demoRampJob?.cancel()
                isDemoActive = false
                demoRampJob = demoScope.launch {
                    while (demoSpeed > 0f) {
                        demoSpeed = (demoSpeed - 0.5f).coerceAtLeast(0f)
                        delay(16)
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        onDemoPress = onDemoPress,
                        onDemoRelease = onDemoRelease,
                        modifier = Modifier.fillMaxHeight().weight(0.4f)
                    )
                    MapPanel(
                        locationData = locationData,
                        routeResult = routeResult,
                        searchState = searchState,
                        pinLocation = pinLocation,
                        onSearch = { viewModel.searchRoute(it) },
                        onClear = { viewModel.clearRoute() },
                        onLongPress = { lat, lng -> viewModel.setPinLocation(lat, lng) },
                        onDirectionsRequested = {
                            pinLocation?.let { (lat, lng) -> viewModel.routeToPin(lat, lng) }
                        },
                        statusMessage = statusMessage,
                        modifier = Modifier.fillMaxHeight().weight(0.6f)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        onDemoPress = onDemoPress,
                        onDemoRelease = onDemoRelease,
                        modifier = Modifier.fillMaxWidth().weight(0.4f)
                    )
                    MapPanel(
                        locationData = locationData,
                        routeResult = routeResult,
                        searchState = searchState,
                        pinLocation = pinLocation,
                        onSearch = { viewModel.searchRoute(it) },
                        onClear = { viewModel.clearRoute() },
                        onLongPress = { lat, lng -> viewModel.setPinLocation(lat, lng) },
                        onDirectionsRequested = {
                            pinLocation?.let { (lat, lng) -> viewModel.routeToPin(lat, lng) }
                        },
                        statusMessage = statusMessage,
                        modifier = Modifier.fillMaxWidth().weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedometerPanel(
    displaySpeed: Float,
    onDemoPress: () -> Unit,
    onDemoRelease: () -> Unit,
    modifier: Modifier
) {
    val currentOnDemoPress = rememberUpdatedState(onDemoPress)
    val currentOnDemoRelease = rememberUpdatedState(onDemoRelease)

    Box(modifier = modifier) {
        SpeedometerGauge(speedKmh = displaySpeed, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0057B8), Color(0xFF003d8a))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, Color(0xFF0066cc), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                currentOnDemoPress.value()
                                tryAwaitRelease()
                                currentOnDemoRelease.value()
                            }
                        )
                    }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HOLD TO DEMO",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun MapPanel(
    locationData: LocationData?,
    routeResult: RouteResult?,
    searchState: SearchState,
    pinLocation: Pair<Double, Double>?,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onLongPress: (Double, Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    statusMessage: String,
    modifier: Modifier
) {
    if (locationData != null) {
        Box(modifier = modifier) {
            MapView(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                bearing = maxOf(0f, locationData.bearing),
                routePoints = routeResult?.points ?: emptyList(),
                pinLocation = pinLocation,
                onLongPress = onLongPress,
                onDirectionsRequested = onDirectionsRequested,
                modifier = Modifier.fillMaxSize()
            )
            // Search bar at bottom — hidden when a route is active
            if (routeResult != null) {
                RouteSummaryBar(
                    routeResult = routeResult,
                    onClear = onClear,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            } else {
                SearchBar(
                    searchState = searchState,
                    onSearch = onSearch,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }
    } else {
        Box(modifier = modifier.background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
            Text(text = statusMessage, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SearchBar(
    searchState: SearchState,
    onSearch: (String) -> Unit,
    modifier: Modifier
) {
    var query by remember { mutableStateOf("") }
    val isLoading = searchState is SearchState.Loading
    val errorMessage = (searchState as? SearchState.Error)?.message

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search destination…", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (query.isNotBlank()) onSearch(query)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00B4D8),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF00B4D8),
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(
                    onClick = { if (query.isNotBlank()) onSearch(query) },
                    enabled = query.isNotBlank()
                ) {
                    Text("Go", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun RouteSummaryBar(
    routeResult: RouteResult,
    onClear: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE6000000), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "%.1f km · %d min".format(routeResult.distanceKm, routeResult.durationMinutes),
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = routeResult.destinationName.take(50),
                color = Color(0xFF445566),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        TextButton(onClick = onClear) {
            Text(
                text = "✕ CLEAR",
                color = Color(0xFFC8102E),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
```

- [ ] **Step 2: Build the full project**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests**

```bash
./gradlew :composeApp:testDebugUnitTest
```
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/sportchronoclock/ui/DashboardScreen.kt
git commit -m "feat: demo controls, bottom search bar, and pin wiring in DashboardScreen"
```
