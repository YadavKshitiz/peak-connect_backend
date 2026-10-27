package com.peakconnect.app.core.navigation

import kotlinx.serialization.Serializable

// --- Root destinations ---
@Serializable
data object BootRoute

@Serializable
data object OnboardingRoute

// --- Auth Graph ---
@Serializable
data object AuthGraphRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

// --- Trekker Graph & Bottom Bar Tabs ---
@Serializable
data object TrekkerGraphRoute

@Serializable
data object ExploreTabRoute

@Serializable
data object SavedTabRoute

@Serializable
data object BookingsTabRoute

@Serializable
data object ProfileTabRoute

// --- Guide Graph & Bottom Bar Tabs ---
@Serializable
data object GuideGraphRoute

@Serializable
data object GuideHomeTabRoute

@Serializable
data object GuideBookingsTabRoute

@Serializable
data object GuideProfileTabRoute

// --- Admin ---
@Serializable
data object AdminUnsupportedRoute

// --- Future Destination Routes (Defined now for later tasks) ---
@Serializable
data class ActivityDetailRoute(val activityId: String)

@Serializable
data class GuideMatchesRoute(val slotId: String, val isWaitlist: Boolean = false)

@Serializable
data class BookingReviewRoute(val slotId: String, val guideId: String? = null)

@Serializable
data class BookingStatusRoute(val bookingId: String)

@Serializable
data class CancelBookingRoute(val bookingId: String)

@Serializable
data class PaymentResultRoute(val bookingId: String)

@Serializable
data object WaitlistRoute

@Serializable
data object NotificationsRoute

@Serializable
data object SettingsRoute

@Serializable
data object GuideProfileEditRoute

@Serializable
data class MapPickerRoute(val initialLat: Double? = null, val initialLng: Double? = null)
