package com.peakconnect.app.data.local

import androidx.room.TypeConverter
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.domain.model.WaitlistStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let {
        try {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let {
        try {
            json.decodeFromString<List<String>>(it)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromBookingStatus(value: BookingStatus?): String? = value?.name

    @TypeConverter
    fun toBookingStatus(value: String?): BookingStatus? = value?.let {
        BookingStatus.entries.find { e -> e.name == it } ?: BookingStatus.PENDING
    }

    @TypeConverter
    fun fromRiskLevel(value: RiskLevel?): String? = value?.name

    @TypeConverter
    fun toRiskLevel(value: String?): RiskLevel? = value?.let {
        RiskLevel.entries.find { e -> e.name == it } ?: RiskLevel.UNKNOWN
    }

    @TypeConverter
    fun fromDifficultyLevel(value: DifficultyLevel?): String? = value?.name

    @TypeConverter
    fun toDifficultyLevel(value: String?): DifficultyLevel? = value?.let {
        DifficultyLevel.entries.find { e -> e.name == it } ?: DifficultyLevel.MODERATE
    }

    @TypeConverter
    fun fromSeason(value: Season?): String? = value?.name

    @TypeConverter
    fun toSeason(value: String?): Season? = value?.let {
        Season.entries.find { e -> e.name == it } ?: Season.LOW
    }

    @TypeConverter
    fun fromExperienceLevel(value: ExperienceLevel?): String? = value?.name

    @TypeConverter
    fun toExperienceLevel(value: String?): ExperienceLevel? = value?.let {
        ExperienceLevel.entries.find { e -> e.name == it } ?: ExperienceLevel.BEGINNER
    }

    @TypeConverter
    fun fromCancellationPolicyType(value: CancellationPolicyType?): String? = value?.name

    @TypeConverter
    fun toCancellationPolicyType(value: String?): CancellationPolicyType? = value?.let {
        CancellationPolicyType.entries.find { e -> e.name == it } ?: CancellationPolicyType.MODERATE
    }

    @TypeConverter
    fun fromWaitlistStatus(value: WaitlistStatus?): String? = value?.name

    @TypeConverter
    fun toWaitlistStatus(value: String?): WaitlistStatus? = value?.let {
        WaitlistStatus.entries.find { e -> e.name == it } ?: WaitlistStatus.WAITING
    }

    @TypeConverter
    fun fromRole(value: Role?): String? = value?.name

    @TypeConverter
    fun toRole(value: String?): Role? = value?.let {
        Role.entries.find { e -> e.name == it } ?: Role.TREKKER
    }
}
