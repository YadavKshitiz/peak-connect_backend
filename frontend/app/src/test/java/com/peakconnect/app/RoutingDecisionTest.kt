package com.peakconnect.app

import com.peakconnect.app.core.navigation.AppDestination
import com.peakconnect.app.core.navigation.resolveInitialDestination
import com.peakconnect.app.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutingDecisionTest {

    @Test
    fun `first launch with onboarding not completed routes to Onboarding`() {
        val destination = resolveInitialDestination(
            onboardingCompleted = false,
            token = "valid_token",
            isTokenExpired = false,
            role = Role.TREKKER
        )
        assertEquals(AppDestination.Onboarding, destination)
    }

    @Test
    fun `logged out with no token routes to Login`() {
        val destinationNull = resolveInitialDestination(
            onboardingCompleted = true,
            token = null,
            isTokenExpired = false,
            role = null
        )
        assertEquals(AppDestination.Login, destinationNull)

        val destinationBlank = resolveInitialDestination(
            onboardingCompleted = true,
            token = "   ",
            isTokenExpired = false,
            role = null
        )
        assertEquals(AppDestination.Login, destinationBlank)
    }

    @Test
    fun `expired token routes to Login`() {
        val destination = resolveInitialDestination(
            onboardingCompleted = true,
            token = "expired_token",
            isTokenExpired = true,
            role = Role.TREKKER
        )
        assertEquals(AppDestination.Login, destination)
    }

    @Test
    fun `valid token with trekker role routes to TrekkerHome`() {
        val destination = resolveInitialDestination(
            onboardingCompleted = true,
            token = "valid_token",
            isTokenExpired = false,
            role = Role.TREKKER
        )
        assertEquals(AppDestination.TrekkerHome, destination)
    }

    @Test
    fun `valid token with guide role routes to GuideHome`() {
        val destination = resolveInitialDestination(
            onboardingCompleted = true,
            token = "valid_token",
            isTokenExpired = false,
            role = Role.GUIDE
        )
        assertEquals(AppDestination.GuideHome, destination)
    }

    @Test
    fun `valid token with admin role routes to AdminUnsupported`() {
        val destination = resolveInitialDestination(
            onboardingCompleted = true,
            token = "valid_token",
            isTokenExpired = false,
            role = Role.ADMIN
        )
        assertEquals(AppDestination.AdminUnsupported, destination)
    }
}
