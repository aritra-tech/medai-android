package com.aritradas.medai

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.emptyPreferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.aritradas.medai.data.datastore.DataStoreUtil
import com.aritradas.medai.domain.repository.BiometricAuthListener
import com.aritradas.medai.navigation.Navigation
import com.aritradas.medai.ui.presentation.splash.SplashViewModel
import com.aritradas.medai.ui.theme.MedAITheme
import com.aritradas.medai.utils.AppBioMetricManager
import com.aritradas.medai.utils.Constants.UPDATE_REQUEST_CODE
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appBioMetricManager: AppBioMetricManager

    private lateinit var splashViewModel: SplashViewModel
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashViewModel = ViewModelProvider(this)[SplashViewModel::class.java]

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        enableEdgeToEdge()
        setContent {
            val context = this@MainActivity
            val themePreference by mainViewModel.themePreference.collectAsState()
            var biometricEnabled by remember { mutableStateOf<Boolean?>(null) }
            // State to track if unlocked (auth success)
            var unlocked by remember { mutableStateOf(false) }

            // Load biometric enabled state from DataStore only once
            LaunchedEffect(Unit) {
                val prefs = try {
                    appBioMetricManager.javaClass.classLoader
                    val dsUtil = DataStoreUtil(context)
                    dsUtil.dataStore.data.first()
                } catch (e: Exception) {
                    emptyPreferences()
                }
                biometricEnabled = prefs[DataStoreUtil.IS_BIOMETRIC_AUTH_SET_KEY] ?: false
            }

            LaunchedEffect(biometricEnabled) {
                if (biometricEnabled == false) unlocked = true
            }

            if (biometricEnabled == true && !unlocked) {
                // Only show lock if biometric is enabled and not already unlocked!
                BiometricLockScreen(
                    mainActivity = this@MainActivity,
                    appBioMetricManager = appBioMetricManager,
                    onSuccess = { unlocked = true },
                    onCancel = {
                        (context as Activity).finish()
                    }
                )
            } else if (biometricEnabled != null) {
                // Safe to show your app as soon as biometricEnabled is read
                LaunchedEffect(Unit) {
                    checkForAppUpdate()
                }
                MedAITheme(
                    themePreference = themePreference
                ) {
                    Navigation(splashViewModel = splashViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForStuckUpdate()
    }

    private fun checkForAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val updateAvailability = appUpdateInfo.updateAvailability()

            if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                val isImmediateUpdateAllowed =
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

                if (isImmediateUpdateAllowed) {
                    Timber.d("Starting immediate update")
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error starting immediate update")
                    }
                } else {
                    Timber.d("Immediate update not allowed")
                }
            } else {
                Timber.d("No update available")
            }
        }.addOnFailureListener { exception ->
            Timber.e(exception, "Error checking for update")
        }
    }

    private fun checkForStuckUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Timber.d("Resuming stuck immediate update")
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error resuming update")
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun BiometricLockScreen(
    mainActivity: MainActivity,
    appBioMetricManager: AppBioMetricManager,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    // Only launch prompt once
    var promptLaunched by remember { mutableStateOf(false) }
    val activity = LocalContext.current as MainActivity

    LaunchedEffect(Unit) {
        if (!promptLaunched) {
            promptLaunched = true
            appBioMetricManager.initBiometricPrompt(
                activity = activity,
                listener = object : BiometricAuthListener {
                    override fun onBiometricAuthSuccess() {
                        onSuccess()
                    }

                    override fun onUserCancelled() {
                        onCancel()
                    }

                    override fun onErrorOccurred() {
                        onCancel()
                    }
                }
            )
        }
    }
    // Blank/Loading UI
    androidx.compose.material3.Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        // Optionally: Add your own loading indicator or lock illustration
    }
}
