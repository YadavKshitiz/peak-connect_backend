package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.semanticColors
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.Difficulty
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season

@Composable
fun AppChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .clip(AppShapes.extraSmall)
            .background(containerColor)
            .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun StatusChip(
    bookingStatus: BookingStatus,
    modifier: Modifier = Modifier
) {
    val colorPair = MaterialTheme.semanticColors.bookingStatusColor(bookingStatus)
    AppChip(
        text = stringResource(id = bookingStatus.labelRes()),
        modifier = modifier,
        containerColor = colorPair.container,
        contentColor = colorPair.content
    )
}

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val colorPair = MaterialTheme.semanticColors.riskColor(riskLevel)
    val icon = when (riskLevel) {
        RiskLevel.LOW -> Icons.Default.Check
        RiskLevel.MODERATE -> Icons.Default.Warning
        RiskLevel.HIGH -> Icons.Default.Dangerous
        RiskLevel.UNKNOWN -> Icons.AutoMirrored.Filled.HelpOutline
    }

    AppChip(
        text = stringResource(id = riskLevel.labelRes()),
        modifier = modifier,
        icon = icon,
        containerColor = colorPair.container,
        contentColor = colorPair.content
    )
}

@Composable
fun DifficultyChip(
    level: Difficulty,
    modifier: Modifier = Modifier
) {
    AppChip(
        text = stringResource(id = level.labelRes()),
        modifier = modifier,
        icon = Icons.Default.Terrain,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@Composable
fun SeasonChip(
    season: Season,
    modifier: Modifier = Modifier
) {
    val icon = when (season) {
        Season.PEAK -> Icons.Default.WbSunny
        Season.SHOULDER -> Icons.Default.WbSunny
        Season.LOW -> Icons.Default.AcUnit
    }

    AppChip(
        text = stringResource(id = season.labelRes()),
        modifier = modifier,
        icon = icon,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "Light Chips", showBackground = true)
@Composable
private fun ChipsPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(bookingStatus = BookingStatus.CONFIRMED)
                StatusChip(bookingStatus = BookingStatus.AWAITING_PAYMENT)
                StatusChip(bookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE)
                StatusChip(bookingStatus = BookingStatus.CANCELLED)
                StatusChip(bookingStatus = BookingStatus.NO_SHOW)
                StatusChip(bookingStatus = BookingStatus.REMATCH_FAILED)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskBadge(riskLevel = RiskLevel.LOW)
                RiskBadge(riskLevel = RiskLevel.MODERATE)
                RiskBadge(riskLevel = RiskLevel.HIGH)
                RiskBadge(riskLevel = RiskLevel.UNKNOWN)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DifficultyChip(level = Difficulty.MODERATE)
                SeasonChip(season = Season.PEAK)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "Dark Chips", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChipsPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(bookingStatus = BookingStatus.CONFIRMED)
                StatusChip(bookingStatus = BookingStatus.AWAITING_PAYMENT)
                RiskBadge(riskLevel = RiskLevel.HIGH)
                DifficultyChip(level = Difficulty.CHALLENGING)
                SeasonChip(season = Season.SHOULDER)
            }
        }
    }
}
