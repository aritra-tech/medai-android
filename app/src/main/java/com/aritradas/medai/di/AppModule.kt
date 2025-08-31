package com.aritradas.medai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aritradas.medai.R
import com.aritradas.medai.data.datastore.DataStoreUtil
import com.aritradas.medai.data.repository.AuthRepositoryImpl
import com.aritradas.medai.data.repository.FeatureRequestRepositoryImpl
import com.aritradas.medai.data.repository.MedicalReportRepositoryImpl
import com.aritradas.medai.data.repository.MedicineDetailsRepositoryImpl
import com.aritradas.medai.data.repository.PrescriptionRepositoryImpl
import com.aritradas.medai.domain.repository.AuthRepository
import com.aritradas.medai.domain.repository.FeatureRequestRepository
import com.aritradas.medai.domain.repository.MedicalReportRepository
import com.aritradas.medai.domain.repository.MedicineDetailsRepository
import com.aritradas.medai.domain.repository.PrescriptionRepository
import com.aritradas.medai.network.GoogleSheetsService
import com.aritradas.medai.network.RetrofitClient
import com.aritradas.medai.utils.AppBioMetricManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context
    ): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        googleSignInClient: GoogleSignInClient
    ): AuthRepository =
        AuthRepositoryImpl(firebaseAuth, googleSignInClient)

    @Provides
    @Singleton
    fun providePrescriptionRepository(
        prescriptionRepositoryImpl: PrescriptionRepositoryImpl
    ): PrescriptionRepository = prescriptionRepositoryImpl

    @Provides
    @Singleton
    fun provideMedicineDetailsRepository(
        impl: MedicineDetailsRepositoryImpl
    ): MedicineDetailsRepository = impl

    @Provides
    @Singleton
    fun provideMedicalReportRepository(
        medicalReportRepositoryImpl: MedicalReportRepositoryImpl
    ): MedicalReportRepository = medicalReportRepositoryImpl

    @Provides
    @Singleton
    fun provideGoogleSheetsService(): GoogleSheetsService = RetrofitClient.googleSheetsService

    @Provides
    @Singleton
    fun provideFeatureRequestRepository(
        googleSheetsService: GoogleSheetsService
    ): FeatureRequestRepository = FeatureRequestRepositoryImpl(googleSheetsService)

    @Provides
    fun provideDataStoreUtil(@ApplicationContext context: Context): DataStoreUtil =
        DataStoreUtil(context)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("user_preferences") }
        )
    }

    @Provides
    fun provideAppBioMetricManager(@ApplicationContext context: Context): AppBioMetricManager {
        return AppBioMetricManager(context)
    }
}
