package com.peakconnect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.peakconnect.app.data.local.entity.ChecklistItemEntity
import com.peakconnect.app.data.local.entity.FavouriteEntity
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.data.local.entity.RecentlyViewedEntity
import com.peakconnect.app.data.local.entity.SearchHistoryEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface UserContentDao {

    // Favourites
    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getFavouritesFlow(): Flow<List<FavouriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE activityId = :activityId)")
    fun isFavouriteFlow(activityId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(favourite: FavouriteEntity): Long

    @Query("DELETE FROM favourites WHERE activityId = :activityId")
    suspend fun deleteFavourite(activityId: String): Int

    // Recently Viewed (keeps last 20)
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT 20")
    fun getRecentlyViewedFlow(): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(entry: RecentlyViewedEntity): Long

    @Query("DELETE FROM recently_viewed WHERE activityId NOT IN (SELECT activityId FROM recently_viewed ORDER BY viewedAt DESC LIMIT 20)")
    suspend fun trimRecentlyViewed(): Int

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun getSearchHistoryFlow(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity): Long

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearch(query: String): Int

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory(): Int

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String): Int

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead(): Int

    // Waitlist
    @Query("SELECT * FROM waitlist_entries ORDER BY joinedAt DESC")
    fun getWaitlistEntriesFlow(): Flow<List<WaitlistEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaitlistEntry(entry: WaitlistEntryEntity): Long

    @Query("DELETE FROM waitlist_entries WHERE slotId = :slotId")
    suspend fun deleteWaitlistEntry(slotId: String): Int

    // Checklists
    @Query("SELECT * FROM checklist_items WHERE (:activityId IS NULL OR activityId = :activityId) AND difficulty = :difficulty ORDER BY id ASC")
    fun getChecklistItemsFlow(activityId: String?, difficulty: DifficultyLevel): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>): List<Long>

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity): Int

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Long): Int

    @Query("DELETE FROM favourites")
    suspend fun clearFavourites(): Int

    @Query("DELETE FROM recently_viewed")
    suspend fun clearRecentlyViewed(): Int

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications(): Int

    @Query("DELETE FROM waitlist_entries")
    suspend fun clearWaitlist(): Int
}

suspend fun UserContentDao.recordView(activityId: String, timestamp: Long = System.currentTimeMillis()) {
    insertRecentlyViewed(RecentlyViewedEntity(activityId, timestamp))
    trimRecentlyViewed()
}

suspend fun UserContentDao.clearUserSpecificData() {
    // Note: Favourites stay intact on logout per specification
    clearRecentlyViewed()
    clearSearchHistory()
    clearNotifications()
    clearWaitlist()
}
