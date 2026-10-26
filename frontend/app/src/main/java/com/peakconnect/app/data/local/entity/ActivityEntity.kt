package com.peakconnect.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.peakconnect.app.domain.model.Activity
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.domain.model.Slot
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "activities",
    indices = [
        Index("location"),
        Index("difficulty"),
        Index("minSlotPrice")
    ]
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val difficulty: DifficultyLevel,
    val basePrice: BigDecimal,
    val cancellationPolicy: CancellationPolicyType,
    val minSlotPrice: BigDecimal?,
    val lastUpdated: Long
)

@Entity(
    tableName = "slots",
    indices = [
        Index("activityId"),
        Index("date")
    ]
)
data class SlotEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val date: LocalDateTime,
    val capacity: Int,
    val currentOccupancy: Int,
    val season: Season,
    val currentPrice: BigDecimal,
    val riskLevel: RiskLevel
)

data class ActivityWithSlots(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val slots: List<SlotEntity>
)

fun SlotEntity.toDomain(): Slot = Slot(
    id = id,
    activityId = activityId,
    date = date,
    capacity = capacity,
    currentOccupancy = currentOccupancy,
    season = season,
    currentPrice = currentPrice,
    riskLevel = riskLevel
)

fun ActivityWithSlots.toDomain(): Activity = Activity(
    id = activity.id,
    title = activity.title,
    description = activity.description,
    location = activity.location,
    latitude = activity.latitude,
    longitude = activity.longitude,
    difficultyLevel = activity.difficulty,
    basePrice = activity.basePrice,
    cancellationPolicy = activity.cancellationPolicy,
    slots = slots.map { it.toDomain() },
    minSlotPrice = activity.minSlotPrice,
    lastUpdated = activity.lastUpdated
)
