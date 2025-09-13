package com.aritradas.medai.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aritradas.medai.MainActivity
import com.aritradas.medai.domain.repository.BiometricAuthListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppBioMetricManager @Inject constructor(@ApplicationContext appContext: Context) {

    private var biometricPrompt: BiometricPrompt? = null
    private val biometricManager = BiometricManager.from(appContext)

    fun canAuthenticate(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                true
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                false
            }

            else -> {
                false
            }
        }
    }

    fun initBiometricPrompt(activity: MainActivity, listener: BiometricAuthListener) {
        biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val cancelled = errorCode in arrayListOf(
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    )
                    if (cancelled) {
                        listener.onUserCancelled()
                    } else {
                        listener.onErrorOccurred()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    listener.onBiometricAuthSuccess()
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setTitle("Unlock MedAI")
            .setSubtitle("Confirm biometric to get logged in")
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt?.authenticate(promptInfo)
    }
}
