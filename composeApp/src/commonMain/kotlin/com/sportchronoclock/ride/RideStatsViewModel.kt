package com.sportchronoclock.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideStatsViewModel(
    private val rideRecorder: RideRecorder,
    private val rideRepository: RideRepository,
) : ViewModel() {

    val liveStats: StateFlow<LiveRideStats?> = rideRecorder.liveStats

    val completedRides: StateFlow<List<RideRecord>> = rideRepository.observeCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rideById(id: Long): StateFlow<RideRecord?> =
        rideRepository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteRide(id: Long) {
        viewModelScope.launch {
            rideRepository.delete(id)
        }
    }
}
