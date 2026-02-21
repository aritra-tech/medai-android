package com.aritradas.medai.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aritradas.medai.ui.presentation.auth.ForgotPasswordScreen
import com.aritradas.medai.ui.presentation.auth.LoginScreen
import com.aritradas.medai.ui.presentation.auth.SignUpScreen
import com.aritradas.medai.ui.presentation.medicalReport.MedicalReportScreen
import com.aritradas.medai.ui.presentation.medicalReportSummarize.MedicalReportSummarizeScreen
import com.aritradas.medai.ui.presentation.medicalReportDetails.MedicalReportDetailsScreen
import com.aritradas.medai.ui.presentation.onboarding.WelcomeScreen
import com.aritradas.medai.ui.presentation.prescription.PrescriptionScreen
import com.aritradas.medai.ui.presentation.prescriptionDetails.PrescriptionDetailsScreen
import com.aritradas.medai.ui.presentation.prescriptionSummarize.PrescriptionSummarizeScreen
import com.aritradas.medai.ui.presentation.pillIdentification.PillIdentificationScreen
import com.aritradas.medai.ui.presentation.profile.HelpScreen
import com.aritradas.medai.ui.presentation.profile.ProfileScreen
import com.aritradas.medai.ui.presentation.settings.SettingsScreen
import com.aritradas.medai.ui.presentation.splash.SplashViewModel

@Composable
fun Navigation(splashViewModel: SplashViewModel) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigationDestination by splashViewModel.navigationDestination.collectAsState()

    LaunchedEffect(navigationDestination) {
        navigationDestination?.let { destination ->
            navController.navigate(destination) {
                popUpTo<Screens.Loading> { inclusive = true }
            }
            splashViewModel.onNavigationComplete()
        }
    }

    val bottomBarScreens = listOf(
        Screens.Prescription::class,
        Screens.MedicalReportScreen::class,
        Screens.Profile::class
    )

    val showBottomBar = bottomBarScreens.any { screenClass ->
        currentDestination?.hasRoute(screenClass) == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.Loading,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screens.Loading> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {}
            }

            composable<Screens.Onboarding> {
                WelcomeScreen(navController)
            }

            composable<Screens.Login> {
                LoginScreen(navController)
            }

            composable<Screens.SignUp> {
                SignUpScreen(
                    navController,
                    onSignUp = {
                        navController.navigate(Screens.Prescription)
                    }
                )
            }

            composable<Screens.Forgot> {
                ForgotPasswordScreen()
            }

            composable<Screens.Prescription> {
                PrescriptionScreen(
                    navController = navController,
                    navigateToDetailsScreen = { id ->
                        navController.navigate(Screens.PrescriptionDetails(id = id))
                    }
                )
            }

            composable<Screens.PrescriptionDetails> { backStackEntry ->
                val prescriptionDetails: Screens.PrescriptionDetails = backStackEntry.toRoute()

                PrescriptionDetailsScreen(
                    navController = navController,
                    prescriptionId = prescriptionDetails.id,
                )
            }

            composable<Screens.PrescriptionSummarize> { backStackEntry ->
                val prescriptionSummarize: Screens.PrescriptionSummarize = backStackEntry.toRoute()

                PrescriptionSummarizeScreen(
                    navController = navController,
                    hasCameraPermission = prescriptionSummarize.hasCameraPermission
                )
            }

            composable<Screens.MedicalReportScreen> {
                MedicalReportScreen(
                    navController = navController,
                )
            }

            composable<Screens.MedicalReportSummarize> { backStackEntry ->

                val medicalReportSummarize: Screens.MedicalReportSummarize = backStackEntry.toRoute()

                MedicalReportSummarizeScreen(
                    navController = navController,
                    hasCameraPermission = medicalReportSummarize.hasCameraPermission
                )
            }

            composable<Screens.MedicalReportDetails> { backStackEntry ->
                val details: Screens.MedicalReportDetails = backStackEntry.toRoute()
                MedicalReportDetailsScreen(
                    navController = navController,
                    reportId = details.id
                )
            }

            composable<Screens.Profile> {
                ProfileScreen(navController = navController)
            }

            composable<Screens.Settings> {
                SettingsScreen(navController = navController)
            }

            composable<Screens.Help> {
                HelpScreen(navController = navController)
            }

            composable<Screens.PillIdentification> {
                PillIdentificationScreen(navController = navController)
            }
        }
    }
}