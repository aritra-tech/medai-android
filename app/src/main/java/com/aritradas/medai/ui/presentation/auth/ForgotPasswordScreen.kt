package com.aritradas.medai.ui.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aritradas.medai.utils.UtilsKt
import com.aritradas.medai.utils.UtilsKt.findActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    val enableSendBtn by remember { derivedStateOf { UtilsKt.validateEmail(email) } }
    val resetPasswordRequestLiveData by authViewModel.resetPassword.observeAsState()
    val errorLiveData by authViewModel.errorLiveData.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(resetPasswordRequestLiveData) {
        if (resetPasswordRequestLiveData == true) {
            context.findActivity()?.let {
                Toast.makeText(it, "Email sent to your registered email", Toast.LENGTH_LONG).show()
                it.finish()
            }
        }
    }

    LaunchedEffect(errorLiveData) {
        errorLiveData?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Forgot Password") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .padding(innerPadding)
                    .imePadding(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            ) {

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your registered email") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            authViewModel.resetPassword(email)
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.resetPassword(email)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    enabled = enableSendBtn && !isLoading
                ) {
                    if (isLoading) {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Send reset link",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}