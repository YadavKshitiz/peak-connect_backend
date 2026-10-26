package com.peakconnect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.domain.model.toLocalDateTimeOrNow
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideDao {

    @Query("SELECT * FROM guide_profile LIMIT 1")
    fun getGuideProfileFlow(): Flow<GuideProfileEntity?>

    @Query("SELECT * FROM guide_profile LIMIT 1")
    suspend fun getGuideProfile(): GuideProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: GuideProfileEntity): Long

    @Query("SELECT * FROM guide_bookings ORDER BY date DESC")
    fun getGuideBookingsFlow(): Flow<List<GuideBookingEntity>>

    @Query("SELECT * FROM guide_bookings")
    suspend fun getAllGuideBookings(): List<GuideBookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuideBooking(booking: GuideBookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuideBookings(bookings: List<GuideBookingEntity>): List<Long>

    @Query("DELETE FROM guide_profile")
    suspend fun clearGuideProfile(): Int

    @Query("DELETE FROM guide_bookings")
    suspend fun clearGuideBookings(): Int
}

suspend fun GuideDao.syncGuideBookings(incoming: List<BookingResponse>, now: Long = System.currentTimeMillis()) {
    val existingMap = getAllGuideBookings().associateBy { it.id }
    val entities = incoming.map { dto ->
        val existing = existingMap[dto.id]
        val statusSince = if (existing != null && existing.status == dto.status) {
            existing.statusSinceMillis
        } else {
            now
        }
        GuideBookingEntity(
            id = dto.id,
            slotId = dto.slotId,
            activityTitle = dto.activityTitle,
            trekkerId = dto.trekkerId,
            trekkerName = dto.trekkerName,
            guideId = dto.guideId,
            guideName = dto.guideName,
            status = dto.status,
            date = dto.date.toLocalDateTimeOrNow(),
            paymentOrderId = dto.paymentOrderId,
            depositAmount = dto.depositAmount,
            refundAmount = dto.refundAmount,
            refundPercentage = dto.refundPercentage,
            statusSinceMillis = statusSince,
            lastSyncMillis = now
        )
    }
    insertGuideBookings(entities)
}
