package com.aritradas.medai.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class AiFunctionsClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseFunctions: FirebaseFunctions
) {

    suspend fun validatePrescription(imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val response = callImageFunction("validate_prescription", imageUri)
        response["valid"] as? Boolean ?: false
    }

    suspend fun summarizePrescription(imageUri: Uri): Map<*, *> = withContext(Dispatchers.IO) {
        val response = callImageFunction("summarize_prescription", imageUri)
        response["summary"] as? Map<*, *> ?: throw IllegalStateException(
            "Unexpected prescription response received from Firebase Functions."
        )
    }

    suspend fun validateMedicalReport(imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val response = callImageFunction("validate_medical_report", imageUri)
        response["valid"] as? Boolean ?: false
    }

    suspend fun summarizeMedicalReport(imageUri: Uri): Map<*, *> = withContext(Dispatchers.IO) {
        val response = callImageFunction("summarize_medical_report", imageUri)
        response["summary"] as? Map<*, *> ?: throw IllegalStateException(
            "Unexpected medical report response received from Firebase Functions."
        )
    }

    private suspend fun callImageFunction(functionName: String, imageUri: Uri): Map<*, *> {
        val image = encodeImage(imageUri)
        return call(
            functionName = functionName,
            payload = mapOf(
                "imageBase64" to image.base64,
                "mimeType" to image.mimeType
            )
        )
    }

    private suspend fun call(functionName: String, payload: Map<String, String>): Map<*, *> {
        val result = firebaseFunctions
            .getHttpsCallable(functionName)
            .call(payload)
            .await()
            .data

        return result as? Map<*, *> ?: throw IllegalStateException(
            "Unexpected response received from $functionName."
        )
    }

    private fun encodeImage(imageUri: Uri): EncodedImagePayload {
        val bitmap = uriToBitmap(imageUri)
        val scaledBitmap = bitmap.scaleDownIfNeeded(MAX_IMAGE_DIMENSION)
        val bytes = scaledBitmap.compressToJpeg()

        return EncodedImagePayload(
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            mimeType = DEFAULT_IMAGE_MIME_TYPE
        )
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            throw Exception("Failed to load image: ${e.message}")
        }
    }

    private fun Bitmap.scaleDownIfNeeded(maxDimension: Int): Bitmap {
        val longestSide = max(width, height)
        if (longestSide <= maxDimension) {
            return this
        }

        val scale = maxDimension.toFloat() / longestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun Bitmap.compressToJpeg(): ByteArray {
        val output = ByteArrayOutputStream()
        var quality = JPEG_QUALITY_START

        while (quality >= JPEG_QUALITY_MIN) {
            output.reset()
            compress(Bitmap.CompressFormat.JPEG, quality, output)
            val bytes = output.toByteArray()
            if (bytes.size <= MAX_IMAGE_BYTES || quality == JPEG_QUALITY_MIN) {
                return bytes
            }
            quality -= JPEG_QUALITY_STEP
        }

        return output.toByteArray()
    }

    private data class EncodedImagePayload(
        val base64: String,
        val mimeType: String
    )

    private companion object {
        const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
        const val MAX_IMAGE_DIMENSION = 1600
        const val MAX_IMAGE_BYTES = 1_500_000
        const val JPEG_QUALITY_START = 85
        const val JPEG_QUALITY_MIN = 55
        const val JPEG_QUALITY_STEP = 10
    }
}
