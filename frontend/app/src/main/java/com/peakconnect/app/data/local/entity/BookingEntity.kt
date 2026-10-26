package com.peakconnect.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import java.time.LocalDateTime

@Entity(
    tableName = "bookings",
    indices = [
        Index("status"),
        Index("date")
    ]
)
data class BookingEntity(
    @PrimaryKey val id: String,
    val slotId: String,
    val activityTitle: String,
    val trekkerId: String,
    val trekkerName: String,
    val guideId: String?, // Nullable
    val guideName: String?, // Nullable
    val status: BookingStatus,
    val date: LocalDateTime,
    val paymentOrderId: String?,
    val depositAmount: Double?,
    val refundAmount: Double?,
    val refundPercentage: Double?,
    val statusSinceMillis: Long,
    val lastSyncMillis: Long
)

@Entity(
    tableName = "guide_bookings",
    indices = [
        Index("status"),
        Index("date")
    ]
)
data class GuideBookingEntity(
    @PrimaryKey val id: String,
    val slotId: String,
    val activityTitle: String,
    val trekkerId: String,
    val trekkerName: String,
    val guideId: String?, // Nullable
    val guideName: String?, // Nullable
    val status: BookingStatus,
    val date: LocalDateTime,
    val paymentOrderId: String?,
    val depositAmount: Double?,
    val refundAmount: Double?,
    val refundPercentage: Double?,
    val statusSinceMillis: Long,
    val lastSyncMillis: Long
)

fun BookingEntity.toDomain(): Booking = Booking(
    id = id,
    slotId = slotId,
    activityTitle = activityTitle,
    trekkerId = trekkerId,
    trekkerName = trekkerName,
    guideId = guideId,
    guideName = guideName,
    status = status,
    date = date,
    paymentOrderId = paymentOrderId,
    depositAmount = depositAmount,
    refundAmount = refundAmount,
    refundPercentage = refundPercentage,
    statusSinceMillis = statusSinceMillis,
    lastSyncMillis = lastSyncMillis
)

fun GuideBookingEntity.toDomain(): Booking = Booking(
    id = id,
    slotId = slotId,
    activityTitle = activityTitle,
    trekkerId = trekkerId,
    trekkerName = trekkerName,
    guideId = guideId,
    guideName = guideName,
    status = status,
    date = date,
    paymentOrderId = paymentOrderId,
    depositAmount = depositAmount,
    refundAmount = refundAmount,
    refundPercentage = refundPercentage,
    statusSinceMillis = statusSinceMillis,
    lastSyncMillis = lastSyncMillis
)
