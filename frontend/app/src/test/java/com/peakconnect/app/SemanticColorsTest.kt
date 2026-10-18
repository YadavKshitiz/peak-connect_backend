package com.peakconnect.app

import com.peakconnect.app.core.theme.DarkSemanticColors
import com.peakconnect.app.core.theme.LightSemanticColors
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.RiskLevel
import org.junit.Assert.assertNotNull
import org.junit.Test

class SemanticColorsTest {

    @Test
    fun riskColor_returnsContainerAndContentColorForAllLevels() {
        for (level in RiskLevel.values()) {
            val lightColor = LightSemanticColors.riskColor(level)
            val darkColor = DarkSemanticColors.riskColor(level)

            assertNotNull(lightColor.container)
            assertNotNull(lightColor.content)
            assertNotNull(darkColor.container)
            assertNotNull(darkColor.content)
        }
    }

    @Test
    fun bookingStatusColor_returnsContainerAndContentColorForAllStatuses() {
        for (status in BookingStatus.values()) {
            val lightColor = LightSemanticColors.bookingStatusColor(status)
            val darkColor = DarkSemanticColors.bookingStatusColor(status)

            assertNotNull(lightColor.container)
            assertNotNull(lightColor.content)
            assertNotNull(darkColor.container)
            assertNotNull(darkColor.content)
        }
    }
}
