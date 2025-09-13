package com.aritradas.medai.ui.presentation.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aritradas.medai.R
import com.aritradas.medai.navigation.Screens
import com.aritradas.medai.utils.UtilsKt.validateEmail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    var backPressedState by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val errorLiveData by authViewModel.errorLiveData.observeAsState()
    val loginSuccess by authViewModel.loginSuccess.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(density) > 0

    val emailError: String? = when {
        !emailTouched -> null
        email.isEmpty() -> "Email is required."
        !validateEmail(email) -> "Please enter a valid email address."
        else -> null
    }
    val passwordError: String? = when {
        !passwordTouched -> null
        password.isEmpty() -> "Password is required."
        else -> null
    }

    val isSignInButtonEnable by remember {
        derivedStateOf {
            validateEmail(email) && password.isNotEmpty() && !isLoading
        }
    }

    LaunchedEffect(errorLiveData) {
        errorLiveData?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(loginSuccess) {
        loginSuccess?.let { success ->
            if (success) {
                navController.navigate(Screens.Prescription) {
                    popUpTo(Screens.Onboarding) { inclusive = true }
                }
            }
        }
    }

    BackHandler {
        if (backPressedState) {
            activity?.finish()
        } else {
            backPressedState = true
            Toast.makeText(
                context,
                context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT
            ).show()

            scope.launch {
                delay(2.seconds)
                backPressedState = false
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Log in") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top
        ) {

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (!emailTouched) emailTouched = true
                },
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = emailError != null,
                supportingText = emailError?.let {
                    {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (!passwordTouched) passwordTouched = true
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password"
                            else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        passwordTouched = true
                        if (isSignInButtonEnable) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            authViewModel.logIn(email, password)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = passwordError != null,
                supportingText = passwordError?.let {
                    {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium
            )

            AnimatedVisibility(!isKeyboardVisible) {
                TextButton(
                    onClick = {
                        navController.navigate(Screens.Forgot)
                    },
                    enabled = !isLoading
                ) {
                    Text(text = "Forgot Password?")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    emailTouched = true
                    passwordTouched = true
                    focusManager.clearFocus()
                    if (isSignInButtonEnable) {
                        authViewModel.logIn(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = isSignInButtonEnable
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    modifier = Modifier.clickable {
                        if (!isLoading) {
                            navController.navigate(Screens.SignUp)
                        }
                    },
                    text = "Create an Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
