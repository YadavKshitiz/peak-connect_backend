package com.peakconnect.app.feature.payment

import android.app.Activity
import android.util.Log
import com.peakconnect.app.BuildConfig
import com.razorpay.Checkout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface PaymentManager {
    fun startPayment(
        activity: Activity,
        activityTitle: String,
        paymentOrderId: String,
        prefillEmail: String?
    )
}

@Singleton
class RazorpayPaymentManager @Inject constructor() : PaymentManager {

    override fun startPayment(
        activity: Activity,
        activityTitle: String,
        paymentOrderId: String,
        prefillEmail: String?
    ) {
        try {
            val checkout = Checkout()
            val keyId = BuildConfig.RAZORPAY_KEY_ID
            if (keyId.isNotBlank()) {
                checkout.setKeyID(keyId)
            }

            val options = JSONObject().apply {
                put("name", "PeakConnect")
                put("description", activityTitle)
                put("order_id", paymentOrderId)
                put("currency", "INR")
                put("theme.color", "#1F6B45")

                val prefill = JSONObject().apply {
                    if (!prefillEmail.isNullOrBlank()) {
                        put("email", prefillEmail)
                    }
                }
                put("prefill", prefill)
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("PaymentManager", "Error opening Razorpay checkout", e)
        }
    }
}
