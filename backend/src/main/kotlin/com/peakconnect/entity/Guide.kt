package com.peakconnect.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "guides")
class Guide(
    @Id
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // Guide uses same ID as User
    @JoinColumn(name = "id")
    // LAZY fetch avoids loading User unless specifically needed when fetching Guide details
    var user: User,
    
    @ElementCollection
    @CollectionTable(name = "guide_skills", joinColumns = [JoinColumn(name = "guide_id")])
    @Column(name = "skill")
    var skills: MutableList<String> = mutableListOf(),

    @ElementCollection
    @CollectionTable(name = "guide_languages", joinColumns = [JoinColumn(name = "guide_id")])
    @Column(name = "language")
    var languages: MutableList<String> = mutableListOf(),

    var location: String? = null,

    @Enumerated(EnumType.STRING)
    var experienceLevel: ExperienceLevel,

    @Column(nullable = false)
    var isVerified: Boolean = false
)
