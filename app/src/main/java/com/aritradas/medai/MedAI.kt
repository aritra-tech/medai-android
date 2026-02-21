package com.aritradas.medai

import android.app.Application
import com.aritradas.medai.utils.MixpanelManager
import com.google.firebase.auth.FirebaseAuth
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MedAI : Application() {
    private var lastRevenueCatUserId: String? = null

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = BuildConfig.REVENUECAT_API_KEY
            ).build()
        )

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val targetUserId = firebaseUser?.uid

            // Avoid repeated RevenueCat login/logout calls for the same auth state.
            if (targetUserId == lastRevenueCatUserId) return@addAuthStateListener

            if (targetUserId != null) {
                Purchases.sharedInstance.logIn(targetUserId, object : LogInCallback {
                    override fun onReceived(
                        customerInfo: com.revenuecat.purchases.CustomerInfo,
                        created: Boolean
                    ) {
                        lastRevenueCatUserId = targetUserId
                        Timber.d(
                            "RevenueCat linked to Firebase uid=%s created=%s activeEntitlements=%s",
                            targetUserId,
                            created,
                            customerInfo.entitlements.active.keys
                        )
                    }

                    override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                        Timber.e(error.underlyingErrorMessage, "RevenueCat logIn failed for uid=%s", targetUserId)
                    }
                })
            } else {
                Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                        lastRevenueCatUserId = null
                        Timber.d(
                            "RevenueCat logged out to anonymous user activeEntitlements=%s",
                            customerInfo.entitlements.active.keys
                        )
                    }

                    override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                        Timber.e(error.underlyingErrorMessage, "RevenueCat logOut failed")
                    }
                })
            }
        }

        MixpanelManager.init(this, BuildConfig.MIXPANEL_PROJECT_TOKEN)
    }
}
