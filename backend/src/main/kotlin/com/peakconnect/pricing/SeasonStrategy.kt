package com.peakconnect.pricing

import com.peakconnect.entity.Season
import com.peakconnect.entity.Slot
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Adjusts price based on the Slot's season.
 * Formula:
 * - LOW: -10% (multiplier 0.90)
 * - SHOULDER: base price (multiplier 1.0)
 * - PEAK: +20% (multiplier 1.20)
 */
@Component
@Order(1) // Runs first
class SeasonStrategy : PricingStrategy {

    override fun calculatePrice(currentPrice: BigDecimal, slot: Slot): BigDecimal {
        val multiplier = when (slot.season) {
            Season.LOW -> BigDecimal("0.90")
            Season.SHOULDER -> BigDecimal("1.00")
            Season.PEAK -> BigDecimal("1.20")
        }
        return currentPrice.multiply(multiplier)
    }
}
