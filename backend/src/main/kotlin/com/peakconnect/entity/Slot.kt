package com.peakconnect.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.Check

@Entity
@Table(name = "slots")
@Check(constraints = "current_occupancy <= capacity") // Database level check constraint
class Slot(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    // LAZY because we don't want to eagerly load the Activity when just listing Slots
    var activity: Activity,

    @Column(nullable = false)
    var date: LocalDateTime,

    @Column(nullable = false)
    var capacity: Int,

    @Column(name = "current_occupancy", nullable = false)
    var currentOccupancy: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var season: Season,

    @OneToMany(mappedBy = "slot", fetch = FetchType.LAZY)
    // LAZY because loading a Slot should not fetch all its bookings by default to avoid N+1 and performance issues
    var bookings: MutableList<Booking> = mutableListOf()
)
