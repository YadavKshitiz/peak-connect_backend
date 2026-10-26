package com.peakconnect.app.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface SessionEvent {
    data object SessionExpired : SessionEvent
}

object SessionEvents {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun emitSessionExpired() {
        _events.tryEmit(SessionEvent.SessionExpired)
    }
}
