package com.aritradas.medai.ui.presentation.subscription

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener

@Composable
fun ProPaywallSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubscribed: () -> Unit
) {
    if (!visible) return

    val paywallListener = remember(onDismiss, onSubscribed) {
        object : PaywallListener {
            override fun onPurchaseCompleted(
                customerInfo: CustomerInfo,
                storeTransaction: StoreTransaction
            ) {
                onSubscribed()
                onDismiss()
            }

            override fun onPurchaseError(error: PurchasesError) {
                Log.e("PAYWALL", "Purchase Error: ${error.message}")
            }

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                onSubscribed()
                onDismiss()
            }

            override fun onRestoreError(error: PurchasesError) {
                Log.e("PAYWALL", "Restore Error: ${error.message}")
            }
        }
    }

    val paywallOptions = remember(onDismiss, paywallListener) {
        PaywallDialogOptions.Builder()
            .setDismissRequest(onDismiss)
            .setShouldDisplayDismissButton(true)
            .setListener(paywallListener)
            .build()
    }

    PaywallDialog(paywallDialogOptions = paywallOptions)
}
