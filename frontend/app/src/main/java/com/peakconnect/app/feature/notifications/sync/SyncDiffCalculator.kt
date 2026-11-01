package com.peakconnect.app.feature.notifications.sync

import com.peakconnect.app.R
import com.peakconnect.app.core.notifications.NotificationChannels
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.toLocalDateTimeOrNow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class PendingNotification(
    val id: String = UUID.randomUUID().toString(),
    val channelId: String,
    val titleRes: Int,
    val titleArgs: List<Any> = emptyList(),
    val bodyRes: Int,
    val bodyArgs: List<Any> = emptyList(),
    val bookingId: String? = null,
    val isGuide: Boolean = false,
    val dedupeKey: String,
    val type: String
)

sealed interface PendingReminder {
    val bookingId: String

    data class PaymentReminder(
        override val bookingId: String,
        val statusSinceMillis: Long
    ) : PendingReminder

    data class GuideResponseReminder(
        override val bookingId: String,
        val firstSeenMillis: Long
    ) : PendingReminder

    data class TrekTomorrowReminder(
        override val bookingId: String,
        val activityTitle: String,
        val slotDateTime: LocalDateTime
    ) : PendingReminder
}

enum class ReminderType {
    PAYMENT,
    GUIDE_RESPONSE,
    TREK_TOMORROW,
    ALL
}

data class CancelReminder(
    val bookingId: String,
    val reminderType: ReminderType
)

data class TrekkerSyncDiffResult(
    val notifications: List<PendingNotification>,
    val updatedBookings: List<BookingEntity>,
    val remindersToSchedule: List<PendingReminder>,
    val remindersToCancel: List<CancelReminder>,
    val promotedWaitlistSlotIds: List<String>
)

data class GuideSyncDiffResult(
    val notifications: List<PendingNotification>,
    val updatedBookings: List<GuideBookingEntity>,
    val remindersToSchedule: List<PendingReminder>,
    val remindersToCancel: List<CancelReminder>
)

object SyncDiffCalculator {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM")

    private fun formatDate(dateTimeString: String): String {
        return try {
            val ldt = dateTimeString.toLocalDateTimeOrNow()
            ldt.format(dateFormatter)
        } catch (_: Exception) {
            dateTimeString
        }
    }

    fun calculateTrekkerDiff(
        previousBookings: List<BookingEntity>,
        incomingBookings: List<BookingResponse>,
        isFirstSync: Boolean,
        activeBookingId: String?,
        alreadyNotifiedKeys: Set<String>,
        waitlistSlotIds: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis()
    ): TrekkerSyncDiffResult {
        val prevMap = previousBookings.associateBy { it.id }

        // Update Room entities
        val updatedEntities = incomingBookings.map { dto ->
            val prev = prevMap[dto.id]
            val statusSince = if (prev != null && prev.status == dto.status) {
                prev.statusSinceMillis
            } else {
                nowMillis
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
                lastSyncMillis = nowMillis
            )
        }

        // Seeding run: Room is populated, but no notifications are emitted
        if (isFirstSync) {
            return TrekkerSyncDiffResult(
                notifications = emptyList(),
                updatedBookings = updatedEntities,
                remindersToSchedule = emptyList(),
                remindersToCancel = emptyList(),
                promotedWaitlistSlotIds = emptyList()
            )
        }

        val notifications = mutableListOf<PendingNotification>()
        val remindersToSchedule = mutableListOf<PendingReminder>()
        val remindersToCancel = mutableListOf<CancelReminder>()
        val promotedWaitlistSlotIds = mutableListOf<String>()

        for (dto in incomingBookings) {
            val isScreenActive = (dto.id == activeBookingId)
            val prev = prevMap[dto.id]

            if (prev == null) {
                // New booking appeared
                if (waitlistSlotIds.contains(dto.slotId)) {
                    // Waitlist promotion
                    val dedupeKey = "waitlist_promoted_${dto.id}"
                    promotedWaitlistSlotIds.add(dto.slotId)
                    if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                        notifications.add(
                            PendingNotification(
                                channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                titleRes = R.string.notif_trekker_waitlist_promoted_title,
                                bodyRes = R.string.notif_trekker_waitlist_promoted_body,
                                bodyArgs = listOf(dto.guideName ?: "your guide"),
                                bookingId = dto.id,
                                isGuide = false,
                                dedupeKey = dedupeKey,
                                type = "BOOKING_UPDATE"
                            )
                        )
                    }
                }
            } else if (prev.status != dto.status) {
                // Status transition
                when (dto.status) {
                    BookingStatus.AWAITING_PAYMENT -> {
                        val dedupeKey = "booking_${dto.id}_AWAITING_PAYMENT"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_trekker_guide_accepted_title,
                                    bodyRes = R.string.notif_trekker_guide_accepted_body,
                                    bookingId = dto.id,
                                    isGuide = false,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToSchedule.add(
                            PendingReminder.PaymentReminder(
                                bookingId = dto.id,
                                statusSinceMillis = nowMillis
                            )
                        )
                    }

                    BookingStatus.CONFIRMED -> {
                        val dedupeKey = "booking_${dto.id}_CONFIRMED"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_trekker_confirmed_title,
                                    bodyRes = R.string.notif_trekker_confirmed_body,
                                    bodyArgs = listOf(dto.activityTitle, formatDate(dto.date)),
                                    bookingId = dto.id,
                                    isGuide = false,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToCancel.add(CancelReminder(dto.id, ReminderType.PAYMENT))
                        remindersToSchedule.add(
                            PendingReminder.TrekTomorrowReminder(
                                bookingId = dto.id,
                                activityTitle = dto.activityTitle,
                                slotDateTime = dto.date.toLocalDateTimeOrNow()
                            )
                        )
                    }

                    BookingStatus.CANCELLED -> {
                        val dedupeKey = "booking_${dto.id}_CANCELLED"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_trekker_cancelled_title,
                                    bodyRes = R.string.notif_trekker_cancelled_body,
                                    bookingId = dto.id,
                                    isGuide = false,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToCancel.add(CancelReminder(dto.id, ReminderType.ALL))
                    }

                    BookingStatus.REMATCH_FAILED -> {
                        val dedupeKey = "booking_${dto.id}_REMATCH_FAILED"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_trekker_rematch_failed_title,
                                    bodyRes = R.string.notif_trekker_rematch_failed_body,
                                    bodyArgs = listOf(dto.activityTitle),
                                    bookingId = dto.id,
                                    isGuide = false,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToCancel.add(CancelReminder(dto.id, ReminderType.ALL))
                    }

                    else -> { /* No notifications needed for other states */ }
                }
            }
        }

        return TrekkerSyncDiffResult(
            notifications = notifications,
            updatedBookings = updatedEntities,
            remindersToSchedule = remindersToSchedule,
            remindersToCancel = remindersToCancel,
            promotedWaitlistSlotIds = promotedWaitlistSlotIds
        )
    }

    fun calculateGuideDiff(
        previousBookings: List<GuideBookingEntity>,
        incomingBookings: List<BookingResponse>,
        isFirstSync: Boolean,
        activeBookingId: String?,
        alreadyNotifiedKeys: Set<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): GuideSyncDiffResult {
        val prevMap = previousBookings.associateBy { it.id }

        val updatedEntities = incomingBookings.map { dto ->
            val prev = prevMap[dto.id]
            val statusSince = if (prev != null && prev.status == dto.status) {
                prev.statusSinceMillis
            } else {
                nowMillis
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
                lastSyncMillis = nowMillis
            )
        }

        if (isFirstSync) {
            return GuideSyncDiffResult(
                notifications = emptyList(),
                updatedBookings = updatedEntities,
                remindersToSchedule = emptyList(),
                remindersToCancel = emptyList()
            )
        }

        val notifications = mutableListOf<PendingNotification>()
        val remindersToSchedule = mutableListOf<PendingReminder>()
        val remindersToCancel = mutableListOf<CancelReminder>()

        for (dto in incomingBookings) {
            val isScreenActive = (dto.id == activeBookingId)
            val prev = prevMap[dto.id]

            if (prev == null) {
                // New booking request for guide
                if (dto.status == BookingStatus.PENDING_GUIDE_RESPONSE) {
                    val dedupeKey = "guide_new_request_${dto.id}"
                    if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                        notifications.add(
                            PendingNotification(
                                channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                titleRes = R.string.notif_guide_new_request_title,
                                bodyRes = R.string.notif_guide_new_request_body,
                                bodyArgs = listOf(dto.activityTitle, formatDate(dto.date)),
                                bookingId = dto.id,
                                isGuide = true,
                                dedupeKey = dedupeKey,
                                type = "BOOKING_UPDATE"
                            )
                        )
                    }
                    remindersToSchedule.add(
                        PendingReminder.GuideResponseReminder(
                            bookingId = dto.id,
                            firstSeenMillis = nowMillis
                        )
                    )
                }
            } else if (prev.status != dto.status) {
                when (dto.status) {
                    BookingStatus.CONFIRMED -> {
                        val dedupeKey = "guide_booking_${dto.id}_CONFIRMED"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_guide_confirmed_title,
                                    bodyRes = R.string.notif_guide_confirmed_body,
                                    bodyArgs = listOf(dto.trekkerName),
                                    bookingId = dto.id,
                                    isGuide = true,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToCancel.add(CancelReminder(dto.id, ReminderType.GUIDE_RESPONSE))
                    }

                    BookingStatus.CANCELLED -> {
                        val dedupeKey = "guide_booking_${dto.id}_CANCELLED"
                        if (!alreadyNotifiedKeys.contains(dedupeKey) && !isScreenActive) {
                            notifications.add(
                                PendingNotification(
                                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                                    titleRes = R.string.notif_guide_cancelled_title,
                                    bodyRes = R.string.notif_guide_cancelled_body,
                                    bookingId = dto.id,
                                    isGuide = true,
                                    dedupeKey = dedupeKey,
                                    type = "BOOKING_UPDATE"
                                )
                            )
                        }
                        remindersToCancel.add(CancelReminder(dto.id, ReminderType.ALL))
                    }

                    else -> { /* Other guide status transitions */ }
                }
            }
        }

        return GuideSyncDiffResult(
            notifications = notifications,
            updatedBookings = updatedEntities,
            remindersToSchedule = remindersToSchedule,
            remindersToCancel = remindersToCancel
        )
    }

    fun calculateTokenExpiryNotification(
        isTokenExpired: Boolean,
        isAlreadyNotified: Boolean,
        tokenExpiryKey: String = "token_expired_session"
    ): PendingNotification? {
        if (!isTokenExpired || isAlreadyNotified) return null
        return PendingNotification(
            channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
            titleRes = R.string.notif_token_expired_title,
            bodyRes = R.string.notif_token_expired_body,
            dedupeKey = tokenExpiryKey,
            type = "TOKEN_EXPIRED"
        )
    }
}
