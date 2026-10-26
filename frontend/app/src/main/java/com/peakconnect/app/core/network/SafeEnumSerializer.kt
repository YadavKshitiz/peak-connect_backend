package com.peakconnect.app.core.network

import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.domain.model.WaitlistStatus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class SafeEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val entries: List<T>,
    private val fallback: T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): T {
        val raw = try {
            decoder.decodeString()
        } catch (_: Exception) {
            return fallback
        }
        return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
    }
}

object BookingStatusSerializer : SafeEnumSerializer<BookingStatus>(
    serialName = "BookingStatus",
    entries = BookingStatus.entries,
    fallback = BookingStatus.PENDING
)

object SafeRiskLevelSerializer : KSerializer<RiskLevel> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RiskLevel", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RiskLevel) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): RiskLevel {
        val raw = try {
            decoder.decodeString()
        } catch (_: Exception) {
            return RiskLevel.UNKNOWN
        }
        return when {
            raw.startsWith("LOW", ignoreCase = true) -> RiskLevel.LOW
            raw.startsWith("MODERATE", ignoreCase = true) -> RiskLevel.MODERATE
            raw.startsWith("HIGH", ignoreCase = true) -> RiskLevel.HIGH
            else -> RiskLevel.UNKNOWN
        }
    }
}

object DifficultyLevelSerializer : SafeEnumSerializer<DifficultyLevel>(
    serialName = "DifficultyLevel",
    entries = DifficultyLevel.entries,
    fallback = DifficultyLevel.MODERATE
)

object SeasonSerializer : SafeEnumSerializer<Season>(
    serialName = "Season",
    entries = Season.entries,
    fallback = Season.LOW
)

object ExperienceLevelSerializer : SafeEnumSerializer<ExperienceLevel>(
    serialName = "ExperienceLevel",
    entries = ExperienceLevel.entries,
    fallback = ExperienceLevel.BEGINNER
)

object CancellationPolicyTypeSerializer : SafeEnumSerializer<CancellationPolicyType>(
    serialName = "CancellationPolicyType",
    entries = CancellationPolicyType.entries,
    fallback = CancellationPolicyType.MODERATE
)

object RoleSerializer : SafeEnumSerializer<Role>(
    serialName = "Role",
    entries = Role.entries,
    fallback = Role.TREKKER
)

object WaitlistStatusSerializer : SafeEnumSerializer<WaitlistStatus>(
    serialName = "WaitlistStatus",
    entries = WaitlistStatus.entries,
    fallback = WaitlistStatus.WAITING
)
