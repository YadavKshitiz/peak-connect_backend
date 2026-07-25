package com.peakconnect.pricing

import com.peakconnect.entity.Slot
import java.math.BigDecimal

/**
 * Interface for all pricing strategies.
 * Design allows for new strategies (e.g., LeadTimeStrategy, DemandStrategy) 
 * to be added in V2 simply by implementing this interface.
 */
interface PricingStrategy {
    /**
     * Calculates the new price based on the strategy's rules.
     *
     * @param currentPrice The price computed so far (allows chaining strategies).
     * @param slot The slot containing data needed for calculation (e.g., season, occupancy).
     * @return The adjusted price.
     */
    fun calculatePrice(currentPrice: BigDecimal, slot: Slot): BigDecimal
}
