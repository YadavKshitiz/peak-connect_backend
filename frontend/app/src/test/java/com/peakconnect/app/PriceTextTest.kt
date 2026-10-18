package com.peakconnect.app

import com.peakconnect.app.core.ui.formatRupees
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PriceTextTest {

    @Test
    fun formatRupees_wholeNumber_formatsWithoutDecimals() {
        val result = formatRupees(BigDecimal("1710"))
        assertEquals("₹1,710", result)
    }

    @Test
    fun formatRupees_withDecimals_formatsWithTwoDecimalPlaces() {
        val result = formatRupees(BigDecimal("1500.50"))
        assertEquals("₹1,500.50", result)
    }

    @Test
    fun formatRupees_lakhAmount_formatsWithIndianGrouping() {
        val result = formatRupees(BigDecimal("100000"))
        assertEquals("₹1,00,000", result)
    }

    @Test
    fun formatRupees_zeroAmount_formatsCorrectly() {
        val result = formatRupees(BigDecimal.ZERO)
        assertEquals("₹0", result)
    }
}
