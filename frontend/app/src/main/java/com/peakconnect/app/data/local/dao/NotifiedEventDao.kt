package com.peakconnect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.peakconnect.app.data.local.entity.NotifiedEventEntity

@Dao
interface NotifiedEventDao {

    @Query("SELECT EXISTS(SELECT 1 FROM notified_events WHERE `key` = :key)")
    suspend fun exists(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotifiedEventEntity): Long

    @Query("DELETE FROM notified_events WHERE notifiedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("DELETE FROM notified_events")
    suspend fun clearAll(): Int
}
