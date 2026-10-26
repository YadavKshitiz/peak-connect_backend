package com.peakconnect.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notified_events")
data class NotifiedEventEntity(
    @PrimaryKey val key: String, // bookingId + "_" + status
    val notifiedAt: Long = System.currentTimeMillis()
)
