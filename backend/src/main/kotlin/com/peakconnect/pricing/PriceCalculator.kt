package com.peakconnect.pricing

import com.peakconnect.entity.Slot
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Composes all available PricingStrategies to calculate the final price of a slot.
 */
@Component
class PriceCalculator(
    private val strategies: List<PricingStrategy>
) {

    /**
     * Calculates the final price by applying all injected strategies sequentially.
     * Order of execution:
     * 1. SeasonStrategy (@Order 1) - base seasonal adjustment
     * 2. OccupancyStrategy (@Order 2) - dynamic markup on top of the seasonal price
     */
    fun calculateFinalPrice(basePrice: BigDecimal, slot: Slot): BigDecimal {
        var currentPrice = basePrice

        for (strategy in strategies) {
            currentPrice = strategy.calculatePrice(currentPrice, slot)
        }

        // Round to 2 decimal places using HALF_UP
        return currentPrice.setScale(2, RoundingMode.HALF_UP)
    }
}
