package com.aritradas.medai.data.repository

import com.aritradas.medai.data.datastore.DataStoreUtil
import com.aritradas.medai.domain.repository.SummaryUsageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SummaryUsageRepositoryImpl @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val auth: FirebaseAuth
) : SummaryUsageRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun observeUsageCount() = dataStoreUtil.getCurrentSummaryUsageCount()

    override suspend fun syncUsageCount(): Int {
        val guestUsage = dataStoreUtil.getGuestSummaryUsageCountOnce()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            dataStoreUtil.setCurrentSummaryUsageCount(guestUsage)
            return guestUsage
        }

        val userRef = firestore.collection("users").document(currentUser.uid)
        val snapshot = userRef.get().await()
        val remoteUsage = snapshot.getLong(FREE_SUMMARY_USAGE_FIELD)?.toInt() ?: 0
        val mergedUsage = maxOf(remoteUsage, guestUsage)

        if (!snapshot.exists() || remoteUsage != mergedUsage) {
            userRef.set(
                mapOf(FREE_SUMMARY_USAGE_FIELD to mergedUsage),
                SetOptions.merge()
            ).await()
        }

        dataStoreUtil.setGuestSummaryUsageCount(0)
        dataStoreUtil.setCurrentSummaryUsageCount(mergedUsage)
        return mergedUsage
    }

    override suspend fun incrementUsageCount(): Int {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            val updatedGuestUsage = dataStoreUtil.incrementGuestSummaryUsageCount()
            dataStoreUtil.setCurrentSummaryUsageCount(updatedGuestUsage)
            return updatedGuestUsage
        }

        val guestUsage = dataStoreUtil.getGuestSummaryUsageCountOnce()
        val userRef = firestore.collection("users").document(currentUser.uid)

        val updatedUsage = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val remoteUsage = snapshot.getLong(FREE_SUMMARY_USAGE_FIELD)?.toInt() ?: 0
            val mergedUsage = maxOf(remoteUsage, guestUsage)
            val newUsage = mergedUsage + 1

            transaction.set(
                userRef,
                mapOf(FREE_SUMMARY_USAGE_FIELD to newUsage),
                SetOptions.merge()
            )

            newUsage
        }.await()

        dataStoreUtil.setGuestSummaryUsageCount(0)
        dataStoreUtil.setCurrentSummaryUsageCount(updatedUsage)
        return updatedUsage
    }

    private companion object {
        const val FREE_SUMMARY_USAGE_FIELD = "freeSummaryUsageCount"
    }
}
