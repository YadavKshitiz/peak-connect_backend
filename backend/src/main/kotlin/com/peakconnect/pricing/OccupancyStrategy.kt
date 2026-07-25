package com.peakconnect.pricing

import com.peakconnect.entity.Slot
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Adjusts price based on current occupancy levels (demand-based pricing).
 * Formula:
 * - < 50% occupancy: base price (multiplier 1.0)
 * - 50% to 80% occupancy: +10% (multiplier 1.10)
 * - >= 80% occupancy: +25% (multiplier 1.25)
 */
@Component
@Order(2) // Runs after SeasonStrategy
class OccupancyStrategy : PricingStrategy {

    override fun calculatePrice(currentPrice: BigDecimal, slot: Slot): BigDecimal {
        val capacity = slot.capacity
        if (capacity == 0) return currentPrice // safety check

        val ratio = slot.currentOccupancy.toDouble() / capacity.toDouble()

        val multiplier = when {
            ratio >= 0.80 -> BigDecimal("1.25")
            ratio >= 0.50 -> BigDecimal("1.10")
            else -> BigDecimal("1.00")
        }

        return currentPrice.multiply(multiplier)
    }
}
