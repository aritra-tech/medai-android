package com.aritradas.medai.domain.repository

import android.net.Uri
import com.aritradas.medai.domain.model.PillIdentification
import com.aritradas.medai.utils.Resource

interface PillRepository {
    suspend fun identifyPill(imageUri: Uri): Resource<PillIdentification>
}
