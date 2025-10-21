package com.aritradas.medai

import android.app.Application
import com.aritradas.medai.utils.MixpanelManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MedAI : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        MixpanelManager.init(this, BuildConfig.MIXPANEL_PROJECT_TOKEN)
    }
}