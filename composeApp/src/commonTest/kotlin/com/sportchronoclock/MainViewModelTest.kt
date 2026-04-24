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
