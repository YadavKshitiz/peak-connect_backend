package com.peakconnect.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.ActivityWithSlots
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.Flow

enum class ActivitySortOption {
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    DIFFICULTY,
    TITLE
}

data class ActivityFilter(
    val query: String? = null,
    val difficulties: Set<DifficultyLevel>? = null,
    val location: String? = null,
    val hasSlotOnDate: String? = null, // yyyy-MM-dd
    val sortOption: ActivitySortOption = ActivitySortOption.TITLE
)

object ActivityQueryBuilder {
    fun buildQuery(filter: ActivityFilter): SimpleSQLiteQuery {
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (!filter.query.isNullOrBlank()) {
            val pattern = "%${filter.query.trim()}%"
            whereClauses.add("(LOWER(title) LIKE LOWER(?) OR LOWER(location) LIKE LOWER(?) OR (description IS NOT NULL AND LOWER(description) LIKE LOWER(?)))")
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }

        if (!filter.location.isNullOrBlank()) {
            whereClauses.add("LOWER(location) = LOWER(?)")
            args.add(filter.location.trim())
        }

        if (!filter.difficulties.isNullOrEmpty()) {
            val placeholders = filter.difficulties.joinToString(",") { "?" }
            whereClauses.add("difficulty IN ($placeholders)")
            filter.difficulties.forEach { args.add(it.name) }
        }

        if (!filter.hasSlotOnDate.isNullOrBlank()) {
            whereClauses.add("id IN (SELECT activityId FROM slots WHERE date LIKE ?)")
            args.add("${filter.hasSlotOnDate.trim()}%")
        }

        val whereSql = if (whereClauses.isNotEmpty()) {
            "WHERE " + whereClauses.joinToString(" AND ")
        } else {
            ""
        }

        val orderSql = when (filter.sortOption) {
            ActivitySortOption.PRICE_LOW_TO_HIGH -> "ORDER BY CAST(minSlotPrice AS REAL) ASC"
            ActivitySortOption.PRICE_HIGH_TO_LOW -> "ORDER BY CAST(minSlotPrice AS REAL) DESC"
            ActivitySortOption.DIFFICULTY -> "ORDER BY difficulty ASC"
            ActivitySortOption.TITLE -> "ORDER BY title ASC"
        }

        val sql = "SELECT * FROM activities $whereSql $orderSql"
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    fun buildCountQuery(filter: ActivityFilter): SimpleSQLiteQuery {
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (!filter.query.isNullOrBlank()) {
            val pattern = "%${filter.query.trim()}%"
            whereClauses.add("(LOWER(title) LIKE LOWER(?) OR LOWER(location) LIKE LOWER(?) OR (description IS NOT NULL AND LOWER(description) LIKE LOWER(?)))")
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }

        if (!filter.location.isNullOrBlank()) {
            whereClauses.add("LOWER(location) = LOWER(?)")
            args.add(filter.location.trim())
        }

        if (!filter.difficulties.isNullOrEmpty()) {
            val placeholders = filter.difficulties.joinToString(",") { "?" }
            whereClauses.add("difficulty IN ($placeholders)")
            filter.difficulties.forEach { args.add(it.name) }
        }

        if (!filter.hasSlotOnDate.isNullOrBlank()) {
            whereClauses.add("id IN (SELECT activityId FROM slots WHERE date LIKE ?)")
            args.add("${filter.hasSlotOnDate.trim()}%")
        }

        val whereSql = if (whereClauses.isNotEmpty()) {
            "WHERE " + whereClauses.joinToString(" AND ")
        } else {
            ""
        }

        val sql = "SELECT COUNT(*) FROM activities $whereSql"
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}

@Dao
interface ActivityDao {

    @RawQuery(observedEntities = [ActivityEntity::class, SlotEntity::class])
    fun getActivitiesPagedRaw(query: SupportSQLiteQuery): PagingSource<Int, ActivityEntity>

    @RawQuery(observedEntities = [ActivityEntity::class, SlotEntity::class])
    suspend fun countActivitiesRaw(query: SupportSQLiteQuery): Int

    @Query("SELECT DISTINCT location FROM activities WHERE location IS NOT NULL AND location != '' ORDER BY location ASC")
    fun getDistinctLocationsFlow(): Flow<List<String>>

    @Query("SELECT a.* FROM activities a INNER JOIN favourites f ON a.id = f.activityId ORDER BY f.addedAt DESC")
    fun getSavedActivitiesFlow(): Flow<List<ActivityEntity>>

    @Query("SELECT a.* FROM activities a INNER JOIN recently_viewed rv ON a.id = rv.activityId ORDER BY rv.viewedAt DESC LIMIT 10")
    fun getRecentlyViewedActivitiesFlow(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY title ASC")
    fun getAllActivitiesFlow(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityFlow(id: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: String): ActivityEntity?

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivityCount(): Int

    @Transaction
    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityWithSlotsFlow(id: String): Flow<ActivityWithSlots?>

    @Transaction
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityWithSlots(id: String): ActivityWithSlots?

    @Query("SELECT * FROM slots WHERE activityId = :activityId ORDER BY date ASC")
    fun getSlotsForActivityFlow(activityId: String): Flow<List<SlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<SlotEntity>): List<Long>

    @Query("DELETE FROM slots WHERE activityId IN (:activityIds)")
    suspend fun deleteSlotsForActivities(activityIds: List<String>): Int

    @Query("DELETE FROM activities")
    suspend fun clearActivities(): Int

    @Query("DELETE FROM slots")
    suspend fun clearSlots(): Int
}

fun ActivityDao.getActivitiesPaged(filter: ActivityFilter = ActivityFilter()): PagingSource<Int, ActivityEntity> {
    return getActivitiesPagedRaw(ActivityQueryBuilder.buildQuery(filter))
}

suspend fun ActivityDao.countActivities(filter: ActivityFilter = ActivityFilter()): Int {
    return countActivitiesRaw(ActivityQueryBuilder.buildCountQuery(filter))
}

suspend fun ActivityDao.syncActivitiesWithSlots(
    activities: List<ActivityEntity>,
    slots: List<SlotEntity>
) {
    val currentIds = activities.map { it.id }
    insertActivities(activities)
    if (currentIds.isNotEmpty()) {
        deleteSlotsForActivities(currentIds)
        insertSlots(slots)
    }
}
