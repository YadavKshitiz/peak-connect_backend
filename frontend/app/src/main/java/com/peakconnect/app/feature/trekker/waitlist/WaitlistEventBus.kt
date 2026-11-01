package com.peakconnect.app.feature.trekker.waitlist

import com.peakconnect.app.domain.model.WaitlistPromoted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface WaitlistEventBus {
    val events: SharedFlow<WaitlistPromoted>
    suspend fun emit(event: WaitlistPromoted)
    fun post(event: WaitlistPromoted)
}

@Singleton
class WaitlistEventBusImpl @Inject constructor() : WaitlistEventBus {
    private val _events = MutableSharedFlow<WaitlistPromoted>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val events: SharedFlow<WaitlistPromoted> = _events.asSharedFlow()

    override suspend fun emit(event: WaitlistPromoted) {
        _events.emit(event)
    }

    override fun post(event: WaitlistPromoted) {
        _events.tryEmit(event)
    }
}
