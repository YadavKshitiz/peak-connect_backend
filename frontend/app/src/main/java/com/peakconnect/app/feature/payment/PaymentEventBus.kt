package com.peakconnect.app.feature.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PaymentEventBus {
    val events: SharedFlow<PaymentEvent>
    fun post(event: PaymentEvent)
    suspend fun emit(event: PaymentEvent)
}

@Singleton
class PaymentEventBusImpl @Inject constructor() : PaymentEventBus {
    private val _events = MutableSharedFlow<PaymentEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    override fun post(event: PaymentEvent) {
        _events.tryEmit(event)
    }

    override suspend fun emit(event: PaymentEvent) {
        _events.emit(event)
    }
}
