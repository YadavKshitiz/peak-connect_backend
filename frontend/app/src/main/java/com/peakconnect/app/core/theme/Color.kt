package com.peakconnect.app.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.RiskLevel

// Brand & Light Theme Palette
val ForestGreenPrimary = Color(0xFF1F6B45)
val ForestGreenOnPrimary = Color(0xFFFFFFFF)
val ForestGreenPrimaryContainer = Color(0xFFCDEBD6)
val ForestGreenOnPrimaryContainer = Color(0xFF08301C)

val GlacierBlueSecondary = Color(0xFF2A7FA8)
val GlacierBlueOnSecondary = Color(0xFFFFFFFF)
val GlacierBlueSecondaryContainer = Color(0xFFD2E9F5)
val GlacierBlueOnSecondaryContainer = Color(0xFF001F2B)

val SunriseOrangeTertiary = Color(0xFFF28C28)
val SunriseOrangeOnTertiary = Color(0xFFFFFFFF)
val SunriseOrangeTertiaryContainer = Color(0xFFFFDCC2)
val SunriseOrangeOnTertiaryContainer = Color(0xFF2E1500)

val LightBackground = Color(0xFFF7F9F7)
val LightOnBackground = Color(0xFF17201A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF17201A)
val LightSurfaceVariant = Color(0xFFE4EBE5)
val LightOnSurfaceVariant = Color(0xFF4A574E)
val LightOutline = Color(0xFF8A978E)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// Dark Theme Palette
val DarkPrimary = Color(0xFF7FD1A0)
val DarkOnPrimary = Color(0xFF003822)
val DarkPrimaryContainer = Color(0xFF0F4D31)
val DarkOnPrimaryContainer = Color(0xFFCDEBD6)

val DarkSecondary = Color(0xFF8CC8E6)
val DarkOnSecondary = Color(0xFF00344A)
val DarkSecondaryContainer = Color(0xFF1C566F)
val DarkOnSecondaryContainer = Color(0xFFD2E9F5)

val DarkTertiary = Color(0xFFFFB066)
val DarkOnTertiary = Color(0xFF4A2800)
val DarkTertiaryContainer = Color(0xFF683B00)
val DarkOnTertiaryContainer = Color(0xFFFFDCC2)

val DarkBackground = Color(0xFF0F1511)
val DarkOnBackground = Color(0xFFE1E8E2)
val DarkSurface = Color(0xFF171E19)
val DarkOnSurface = Color(0xFFE1E8E2)
val DarkSurfaceVariant = Color(0xFF2A332D)
val DarkOnSurfaceVariant = Color(0xFFBFC9C1)
val DarkOutline = Color(0xFF89938B)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)

val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = ForestGreenOnPrimary,
    primaryContainer = ForestGreenPrimaryContainer,
    onPrimaryContainer = ForestGreenOnPrimaryContainer,
    secondary = GlacierBlueSecondary,
    onSecondary = GlacierBlueOnSecondary,
    secondaryContainer = GlacierBlueSecondaryContainer,
    onSecondaryContainer = GlacierBlueOnSecondaryContainer,
    tertiary = SunriseOrangeTertiary,
    onTertiary = SunriseOrangeOnTertiary,
    tertiaryContainer = SunriseOrangeTertiaryContainer,
    onTertiaryContainer = SunriseOrangeOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError
)

// Semantic Colors for Status, Risk, and Alerts
data class StatusColor(
    val container: Color,
    val content: Color
)

@Immutable
data class SemanticColors(
    val riskLow: Color,
    val riskModerate: Color,
    val riskHigh: Color,
    val riskUnknown: Color,
    val statusPendingGuideResponse: Color,
    val statusAwaitingPayment: Color,
    val statusConfirmed: Color,
    val statusCancelled: Color,
    val statusNoShow: Color,
    val statusRematchFailed: Color,
    val statusPending: Color,
    val isDark: Boolean
) {
    fun riskColor(level: RiskLevel): StatusColor {
        val base = when (level) {
            RiskLevel.LOW -> riskLow
            RiskLevel.MODERATE -> riskModerate
            RiskLevel.HIGH -> riskHigh
            RiskLevel.UNKNOWN -> riskUnknown
        }
        val container = if (isDark) base.copy(alpha = 0.24f) else base.copy(alpha = 0.16f)
        val content = if (isDark) {
            when (level) {
                RiskLevel.LOW -> Color(0xFF7EE7AA)
                RiskLevel.MODERATE -> Color(0xFFFFD47F)
                RiskLevel.HIGH -> Color(0xFFFFB4AB)
                RiskLevel.UNKNOWN -> Color(0xFFC7D0CB)
            }
        } else {
            when (level) {
                RiskLevel.LOW -> Color(0xFF136838)
                RiskLevel.MODERATE -> Color(0xFF8A6200)
                RiskLevel.HIGH -> Color(0xFF9E1D1D)
                RiskLevel.UNKNOWN -> Color(0xFF4A574E)
            }
        }
        return StatusColor(container = container, content = content)
    }

    fun bookingStatusColor(status: BookingStatus): StatusColor {
        val base = when (status) {
            BookingStatus.PENDING_GUIDE_RESPONSE -> statusPendingGuideResponse
            BookingStatus.AWAITING_PAYMENT -> statusAwaitingPayment
            BookingStatus.CONFIRMED -> statusConfirmed
            BookingStatus.CANCELLED -> statusCancelled
            BookingStatus.NO_SHOW -> statusNoShow
            BookingStatus.REMATCH_FAILED -> statusRematchFailed
            BookingStatus.PENDING -> statusPending
        }
        val container = if (isDark) base.copy(alpha = 0.24f) else base.copy(alpha = 0.16f)
        val content = if (isDark) {
            when (status) {
                BookingStatus.PENDING_GUIDE_RESPONSE -> Color(0xFFFFD47F)
                BookingStatus.AWAITING_PAYMENT -> Color(0xFF8CC8E6)
                BookingStatus.CONFIRMED -> Color(0xFF7EE7AA)
                BookingStatus.CANCELLED -> Color(0xFFBFC9C1)
                BookingStatus.NO_SHOW -> Color(0xFFFFB4AB)
                BookingStatus.REMATCH_FAILED -> Color(0xFFFFB088)
                BookingStatus.PENDING -> Color(0xFFBFC9C1)
            }
        } else {
            when (status) {
                BookingStatus.PENDING_GUIDE_RESPONSE -> Color(0xFF8A6200)
                BookingStatus.AWAITING_PAYMENT -> Color(0xFF1B6286)
                BookingStatus.CONFIRMED -> Color(0xFF136838)
                BookingStatus.CANCELLED -> Color(0xFF4A574E)
                BookingStatus.NO_SHOW -> Color(0xFF9E1D1D)
                BookingStatus.REMATCH_FAILED -> Color(0xFFA53A00)
                BookingStatus.PENDING -> Color(0xFF4A574E)
            }
        }
        return StatusColor(container = container, content = content)
    }
}

val LightSemanticColors = SemanticColors(
    riskLow = Color(0xFF2E9E5B),
    riskModerate = Color(0xFFE0A100),
    riskHigh = Color(0xFFD64545),
    riskUnknown = Color(0xFF7A8580),
    statusPendingGuideResponse = Color(0xFFE0A100),
    statusAwaitingPayment = Color(0xFF2A7FA8),
    statusConfirmed = Color(0xFF2E9E5B),
    statusCancelled = Color(0xFF7A8580),
    statusNoShow = Color(0xFFD64545),
    statusRematchFailed = Color(0xFFE8590C),
    statusPending = Color(0xFF7A8580),
    isDark = false
)

val DarkSemanticColors = SemanticColors(
    riskLow = Color(0xFF2E9E5B),
    riskModerate = Color(0xFFE0A100),
    riskHigh = Color(0xFFD64545),
    riskUnknown = Color(0xFF7A8580),
    statusPendingGuideResponse = Color(0xFFE0A100),
    statusAwaitingPayment = Color(0xFF2A7FA8),
    statusConfirmed = Color(0xFF2E9E5B),
    statusCancelled = Color(0xFF7A8580),
    statusNoShow = Color(0xFFD64545),
    statusRematchFailed = Color(0xFFE8590C),
    statusPending = Color(0xFF7A8580),
    isDark = true
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
