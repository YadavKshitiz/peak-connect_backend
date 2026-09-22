package com.peakconnect.pricing

import com.peakconnect.entity.Slot
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Adjusts price based on recent search/interest volume for the slot.
 * - >100 views in 24h: premium multiplier (e.g. 1.10x)
 * - Otherwise: neutral (1.0x)
 */
@Component
@Order(4) // Runs after LeadTimeStrategy
class DemandStrategy(
    private val demandTrackerService: DemandTrackerService,
    @Value("\${pricing.demand.high-demand-threshold:100}")
    private val highDemandThreshold: Long,
    @Value("\${pricing.demand.high-demand-multiplier:1.10}")
    private val highDemandMultiplier: String
) : PricingStrategy {

    override fun calculatePrice(currentPrice: BigDecimal, slot: Slot): BigDecimal {
        if (slot.id == null) return currentPrice // Safeguard for unsaved slots

        val recentViews = demandTrackerService.getSlotViews(slot.id!!)

        val multiplier = if (recentViews > highDemandThreshold) {
            BigDecimal(highDemandMultiplier)
        } else {
            BigDecimal("1.00")
        }

        return currentPrice.multiply(multiplier)
    }
}
