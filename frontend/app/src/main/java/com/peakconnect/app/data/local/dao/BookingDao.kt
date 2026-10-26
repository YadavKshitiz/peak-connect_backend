package com.peakconnect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.domain.model.toLocalDateTimeOrNow
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings ORDER BY date DESC")
    fun getMyBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    fun getBookingFlow(id: String): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings")
    suspend fun getAllBookings(): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>): List<Long>

    @Query("DELETE FROM bookings")
    suspend fun clearBookings(): Int
}

suspend fun BookingDao.syncBookings(incoming: List<BookingResponse>, now: Long = System.currentTimeMillis()) {
    val existingMap = getAllBookings().associateBy { it.id }
    val entities = incoming.map { dto ->
        val existing = existingMap[dto.id]
        val statusSince = if (existing != null && existing.status == dto.status) {
            existing.statusSinceMillis
        } else {
            now
        }
        BookingEntity(
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
    insertBookings(entities)
}
