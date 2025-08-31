package com.aritradas.medai.network

import com.aritradas.medai.BuildConfig
import kotlin.jvm.java
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.fda.gov/drug/"
    private const val GOOGLE_SHEETS_BASE_URL = "https://sheets.googleapis.com/"
    private const val API_KEY = BuildConfig.API_KEY

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()
            chain.proceed(request)
        })
        .build()

    private val googleSheetsClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        })
        .build()

    val openFdaService: MedAIService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(MedAIService::class.java)
    }

    val googleSheetsService: GoogleSheetsService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_SHEETS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(googleSheetsClient)
            .build()
            .create(GoogleSheetsService::class.java)
    }
}
