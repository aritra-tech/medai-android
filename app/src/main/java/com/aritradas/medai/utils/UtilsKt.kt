package com.aritradas.medai.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Patterns
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

object UtilsKt {

    fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun validateName(name: String?): Boolean {
        if (name.isNullOrEmpty() || name.trim().isEmpty()) {
            return false
        }

        val trimmedName = name.trim()

        // Check minimum length
        if (trimmedName.length < 2) {
            return false
        }

        // Check if name contains only letters, spaces, and dots
        val namePattern = "^[A-Za-z.\\s]+$"
        if (!trimmedName.matches(Regex(namePattern))) {
            return false
        }

        // Check that each word is valid (no consecutive spaces or dots)
        return trimmedName.split("\\s+".toRegex())
            .all { word ->
                word.isNotEmpty() &&
                        word.matches(Regex("^[A-Za-z]+\\.?[A-Za-z]*$"))
            }
    }



    fun validateEmail(email: String?): Boolean {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(pw: String): Boolean {
        return pw.length >= 8 && pw.any { it.isLetter() } && pw.any { it.isDigit() }
    }

    fun getInitials(name: String): String {
        val words = name.trim().split(" ")
        return when {
            words.size == 1 -> words[0].take(2)
            words.size >= 2 -> words[0].take(1) + words[1].take(1)
            else -> ""
        }
    }
}
