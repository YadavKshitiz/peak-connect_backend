package com.peakconnect.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "bookings")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    // LAZY fetch to prevent pulling in Slot when we only need the Booking info
    var slot: Slot,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trekker_id", nullable = false)
    // LAZY fetch avoids loading trekker's full user profile unless specifically accessed
    var trekker: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    // LAZY fetch avoids loading Guide details (skills, user) unless necessary
    var guide: Guide? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BookingStatus = BookingStatus.PENDING,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
