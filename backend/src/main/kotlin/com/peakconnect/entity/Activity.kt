package com.peakconnect.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "activities")
class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var location: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var difficultyLevel: DifficultyLevel,

    @Column(nullable = false)
    var basePrice: BigDecimal,

    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    // LAZY is used because we don't always need to load all slots when showing activity details, avoiding N+1
    var slots: MutableList<Slot> = mutableListOf()
)
