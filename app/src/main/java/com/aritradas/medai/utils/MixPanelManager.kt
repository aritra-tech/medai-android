package com.aritradas.medai.utils

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI

object MixpanelManager {

    private var mixpanel: MixpanelAPI? = null

    fun init(context: Context, token: String) {
        synchronized(this) {
            if (mixpanel == null) {
                mixpanel = MixpanelAPI.getInstance(context, token, false)
            }
        }
    }

    fun trackSignupCompleted() {
        mixpanel?.track("signup_completed")
    }

    fun trackPrescriptionSummarization() {
        mixpanel?.track("prescription_summarization")
    }

    fun trackMedicalReportSummarization() {
        mixpanel?.track("medical_report_summarization")
    }
}