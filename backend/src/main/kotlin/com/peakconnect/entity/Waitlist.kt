package com.peakconnect.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "waitlists")
class Waitlist(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    var slot: Slot,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trekker_id", nullable = false)
    var trekker: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    var guide: Guide? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    var booking: Booking? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WaitlistStatus = WaitlistStatus.WAITING,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "promoted_at")
    var promotedAt: LocalDateTime? = null
)
