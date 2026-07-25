package com.peakconnect.repository

import com.peakconnect.entity.Slot
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SlotRepository : JpaRepository<Slot, UUID> {
    fun findByActivityId(activityId: UUID): List<Slot>
}
