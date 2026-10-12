package com.peakconnect.dto

import java.util.UUID

data class BookingsTodayStats(
    val created: Int,
    val confirmed: Int
)

data class CacheStats(
    val hits: Long,
    val misses: Long,
    val hitRate: Double
)

data class SlotWaitlistStats(
    val waiting: Int,
    val promoted: Int
)

data class WaitlistStats(
    val totalWaiting: Int,
    val totalPromoted: Int,
    val breakdownBySlot: Map<UUID, SlotWaitlistStats>
)

data class GuideResponseStats(
    val totalAssignments: Int,
    val accepted: Int,
    val declined: Int,
    val timedOut: Int,
    val acceptRate: Double,
    val declineRate: Double
)
