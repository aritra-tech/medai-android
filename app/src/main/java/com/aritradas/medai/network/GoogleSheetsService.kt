package com.aritradas.medai.network

import com.aritradas.medai.domain.model.GoogleSheetsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleSheetsService {
    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendValues(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Query("key") apiKey: String,
        @Body request: GoogleSheetsRequest
    ): Response<Any>
}