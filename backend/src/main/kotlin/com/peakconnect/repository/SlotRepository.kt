package com.peakconnect.repository

import com.peakconnect.entity.Slot
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SlotRepository : JpaRepository<Slot, UUID> {
    fun findByActivityId(activityId: UUID): List<Slot>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    fun findSlotByIdForUpdate(@Param("id") id: UUID): Optional<Slot>
}
