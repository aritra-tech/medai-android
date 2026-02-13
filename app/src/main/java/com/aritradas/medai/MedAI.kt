package com.aritradas.medai

import android.app.Application
import com.aritradas.medai.utils.MixpanelManager
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MedAI : Application() {
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
        MixpanelManager.init(this, BuildConfig.MIXPANEL_PROJECT_TOKEN)
    }
}
