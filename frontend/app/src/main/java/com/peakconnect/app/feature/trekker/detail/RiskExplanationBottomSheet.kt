package com.peakconnect.app.feature.trekker.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.ui.AppModalBottomSheet
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.RiskBadge
import com.peakconnect.app.domain.model.RiskLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskExplanationBottomSheet(
    riskLevel: RiskLevel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.risk_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.common_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RiskBadge(riskLevel = riskLevel)

            Spacer(modifier = Modifier.height(16.dp))

            val explanationRes = when (riskLevel) {
                RiskLevel.LOW -> R.string.risk_level_low_explanation
                RiskLevel.MODERATE -> R.string.risk_level_moderate_explanation
                RiskLevel.HIGH -> R.string.risk_level_high_explanation
                RiskLevel.UNKNOWN -> R.string.risk_level_unknown_explanation
            }

            Text(
                text = stringResource(id = explanationRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(id = R.string.common_close),
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
