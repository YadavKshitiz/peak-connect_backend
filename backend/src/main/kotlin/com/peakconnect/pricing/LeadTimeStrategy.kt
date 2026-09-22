package com.peakconnect.pricing

import com.peakconnect.entity.Slot
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Adjusts price based on how far in advance the booking is being made.
 * - >30 days out: small discount (e.g. 0.95x)
 * - <3 days out: premium multiplier (e.g. 1.15x)
 * - Otherwise: neutral (1.0x)
 */
@Component
@Order(3) // Runs after SeasonStrategy and OccupancyStrategy
class LeadTimeStrategy(
    @Value("\${pricing.lead-time.far-advance-days:30}")
    private val farAdvanceDays: Long,
    @Value("\${pricing.lead-time.far-advance-multiplier:0.95}")
    private val farAdvanceMultiplier: String,
    
    @Value("\${pricing.lead-time.last-minute-days:3}")
    private val lastMinuteDays: Long,
    @Value("\${pricing.lead-time.last-minute-multiplier:1.15}")
    private val lastMinuteMultiplier: String
) : PricingStrategy {

    override fun calculatePrice(currentPrice: BigDecimal, slot: Slot): BigDecimal {
        val daysUntilActivity = ChronoUnit.DAYS.between(LocalDateTime.now(), slot.date)

        val multiplier = when {
            daysUntilActivity > farAdvanceDays -> BigDecimal(farAdvanceMultiplier)
            daysUntilActivity < lastMinuteDays && daysUntilActivity >= 0 -> BigDecimal(lastMinuteMultiplier)
            else -> BigDecimal("1.00")
        }

        return currentPrice.multiply(multiplier)
    }
}
