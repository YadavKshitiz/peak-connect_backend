package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.peakconnect.app.core.theme.PeakConnectTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats a [BigDecimal] amount in Indian Rupees (INR) using Indian numbering system (en-IN).
 * - Amounts with zero fraction digits (e.g. 1710 or 1710.00) format as "₹1,710".
 * - Amounts with non-zero decimal values (e.g. 1500.50) format with two decimal digits as "₹1,500.50".
 */
fun formatRupees(amount: BigDecimal): String {
    val isNegative = amount.signum() < 0
    val absAmount = amount.abs()
    val scaled = absAmount.setScale(2, java.math.RoundingMode.HALF_UP)

    val wholePart = scaled.toBigInteger().toString()
    val formattedWhole = if (wholePart.length <= 3) {
        wholePart
    } else {
        val last3 = wholePart.takeLast(3)
        val remaining = wholePart.dropLast(3)
        val groupedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$groupedRemaining,$last3"
    }

    val hasFraction = scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
    val result = if (hasFraction) {
        val fractionPart = scaled.remainder(BigDecimal.ONE).multiply(BigDecimal(100)).toInt()
        val fractionString = String.format(Locale.forLanguageTag("en-IN"), "%02d", fractionPart)
        "₹$formattedWhole.$fractionString"
    } else {
        "₹$formattedWhole"
    }

    return if (isNegative) "-$result" else result
}

@Composable
fun PriceText(
    amount: BigDecimal,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        text = formatRupees(amount),
        modifier = modifier,
        style = style.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Preview(name = "Light PriceText", showBackground = true)
@Composable
private fun PriceTextPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        PriceText(amount = BigDecimal("1710"))
    }
}

@Preview(name = "Dark PriceText", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PriceTextPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        PriceText(amount = BigDecimal("1500.50"))
    }
}
