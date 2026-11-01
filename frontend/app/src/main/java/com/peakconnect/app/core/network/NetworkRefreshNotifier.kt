package com.peakconnect.app.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton notifier that coordinates screen data refresh when the app transitions back online.
 */
@Singleton
class NetworkRefreshNotifier @Inject constructor() {

    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<Unit> = _refreshEvents.asSharedFlow()

    fun notifyBackOnline() {
        _refreshEvents.tryEmit(Unit)
    }

    fun triggerRefresh() {
        notifyBackOnline()
    }
}
