package com.peakconnect.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.WaitlistEntry
import com.peakconnect.app.domain.model.WaitlistStatus
import java.time.LocalDateTime

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val activityId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val activityId: String,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val message: String,
    val bookingId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "waitlist_entries")
data class WaitlistEntryEntity(
    @PrimaryKey val slotId: String,
    val activityTitle: String,
    val slotDate: LocalDateTime,
    val joinedAt: Long = System.currentTimeMillis(),
    val status: WaitlistStatus = WaitlistStatus.WAITING
)

fun WaitlistEntryEntity.toDomain(): WaitlistEntry = WaitlistEntry(
    slotId = slotId,
    activityTitle = activityTitle,
    slotDate = slotDate,
    joinedAt = joinedAt,
    status = status
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String? = null,
    val difficulty: DifficultyLevel,
    val text: String,
    val isChecked: Boolean = false,
    val isCustom: Boolean = false
)
