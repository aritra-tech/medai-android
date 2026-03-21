package com.aritradas.medai.data.repository

import com.aritradas.medai.domain.repository.AuthRepository
import com.aritradas.medai.utils.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Resource<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            Resource.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()

            if (e is FirebaseAuthException) {
                return when (e.errorCode) {
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> {
                        Resource.Error("An account with this email already exists with a different sign-in method.")
                    }

                    "ERROR_INVALID_CREDENTIAL" -> {
                        Resource.Error("Invalid Google credential provided.")
                    }

                    else -> {
                        Resource.Error(e.localizedMessage ?: "Firebase Google Sign-In error occurred.")
                    }
                }
            } else {
                return Resource.Error(e.localizedMessage ?: "An unexpected error occurred during Google Sign-In.")
            }
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        return try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred during sign out.")
        }
    }

    override fun getCurrentUser() = firebaseAuth.currentUser

    override suspend fun getUserNameFromFirestore(userId: String): String? {
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            listOf("name", "username", "a")
                .asSequence()
                .mapNotNull(document::getString)
                .map(String::trim)
                .firstOrNull(String::isNotBlank)
        } catch (e: Exception) {
            null
        }
    }
}
