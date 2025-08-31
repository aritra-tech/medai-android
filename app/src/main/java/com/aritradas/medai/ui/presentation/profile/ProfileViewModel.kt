package com.aritradas.medai.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.domain.model.FeatureRequest
import com.aritradas.medai.domain.model.UserData
import com.aritradas.medai.domain.repository.AuthRepository
import com.aritradas.medai.domain.repository.FeatureRequestRepository
import com.aritradas.medai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureRequestRepository: FeatureRequestRepository
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    private val _featureRequestState = MutableStateFlow<Resource<String>?>(null)
    val featureRequestState = _featureRequestState.asStateFlow()

    init {
        viewModelScope.launch {
            loadUserData()
        }
    }

    private suspend fun loadUserData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            val userNameFromFirestore = authRepository.getUserNameFromFirestore(currentUser.uid)
            _userData.value = UserData(
                userId = currentUser.uid,
                username = userNameFromFirestore ?: currentUser.displayName, 
                profilePictureUrl = currentUser.photoUrl?.toString()
            )
        }
    }

    fun submitFeatureRequest(name: String, email: String, featureDetail: String) {
        viewModelScope.launch {
            _featureRequestState.value = Resource.Loading()

            val timestamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val featureRequest = FeatureRequest(
                name = name,
                email = email,
                featureDetail = featureDetail,
                timestamp = timestamp
            )

            val result = featureRequestRepository.submitFeatureRequest(featureRequest)
            _featureRequestState.value = result
        }
    }

    fun clearFeatureRequestState() {
        _featureRequestState.value = null
    }
}