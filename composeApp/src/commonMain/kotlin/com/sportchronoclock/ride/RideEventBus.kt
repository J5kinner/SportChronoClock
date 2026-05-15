package com.sportchronoclock.ride

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RideEventBus {
    private val _events = MutableSharedFlow<RideEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<RideEvent> = _events.asSharedFlow()

    fun tryEmit(event: RideEvent): Boolean = _events.tryEmit(event)

    suspend fun emit(event: RideEvent) = _events.emit(event)
}
