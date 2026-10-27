package com.peakconnect.app.feature.trekker.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.peakconnect.app.core.ui.formatRupees
import java.math.BigDecimal

object ActivityDetailUtils {

    const val GITHUB_REPO_URL = "https://github.com/YadavKshitiz/peak-connect_backend"

    fun buildShareText(title: String, location: String, lowestPrice: BigDecimal): String {
        val formattedPrice = formatRupees(lowestPrice)
        return "$title in $location — from $formattedPrice on PeakConnect\n$GITHUB_REPO_URL"
    }

    fun buildGeoIntentUri(latitude: Double, longitude: Double, title: String): Uri {
        val encodedTitle = Uri.encode(title)
        return Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedTitle)")
    }

    fun openMapDirections(context: Context, latitude: Double, longitude: Double, title: String): Boolean {
        val uri = buildGeoIntentUri(latitude, longitude, title)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openShareSheet(context: Context, shareText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
