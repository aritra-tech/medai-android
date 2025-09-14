package com.aritradas.medai.ui.presentation.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.aritradas.medai.utils.MixpanelManager
import com.aritradas.medai.utils.UtilsKt.validateEmail
import com.aritradas.medai.utils.UtilsKt.validateName
import com.aritradas.medai.utils.UtilsKt.validatePassword
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    onSignUp: (FirebaseUser) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    var backPressedState by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    val errorLiveData by authViewModel.errorLiveData.observeAsState()
    val registerStatus by authViewModel.registerStatus.observeAsState(false)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading by authViewModel.isLoading.observeAsState(false)
    val scrollState = rememberScrollState()
    var userNameTouched by remember { mutableStateOf(false) }
    var userEmailTouched by remember { mutableStateOf(false) }
    var userPasswordTouched by remember { mutableStateOf(false) }



    val userNameError: String? = when {
        !userNameTouched -> null
        userName.isEmpty() -> "Name is required."
        !validateName(userName) -> "Please enter a valid name (letters only)."
        else -> null
    }
    val userEmailError: String? = when {
        !userEmailTouched -> null
        userEmail.isEmpty() -> "Email is required."
        !validateEmail(userEmail) -> "Please enter a valid email address."
        else -> null
    }
    val userPasswordError: String? = when {
        !userPasswordTouched -> null
        userPassword.isEmpty() -> "Password is required."
        !validatePassword(userPassword) -> "Password must be at least 8 characters and contain both letters and numbers."
        else -> null
    }

    val isSignUpButtonEnabled by remember {
        derivedStateOf {
            validateName(userName) && validateEmail(userEmail) && validatePassword(userPassword) && !isLoading
        }
    }

    LaunchedEffect(registerStatus) {
        if (registerStatus) {
            Toast.makeText(
                context,
                "Account created successfully", Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(errorLiveData) {
        errorLiveData?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (backPressedState) {
            activity?.finish()
        } else {
            backPressedState = true
            Toast.makeText(context,
                context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()

            scope.launch {
                delay(2.seconds)
                backPressedState = false
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Sign Up") },
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
                value = userName,
                onValueChange = {
                    userName = it
                    if (!userNameTouched) userNameTouched = true
                },
                label = { Text("Name") },
                placeholder = { Text("Enter your name") },
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
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
                isError = userNameError != null,
                supportingText = userNameError?.let {
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
                value = userEmail,
                onValueChange = {
                    userEmail = it
                    if (!userEmailTouched) userEmailTouched = true
                },
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
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
                isError = userEmailError != null,
                supportingText = userEmailError?.let {
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
                value = userPassword,
                onValueChange = {
                    userPassword = it
                    if (!userPasswordTouched) userPasswordTouched = true
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
                        userPasswordTouched = true
                        if (isSignUpButtonEnabled) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            authViewModel.signUp(
                                userName,
                                userEmail,
                                userPassword,
                                onSignedUp = { signUpUser ->
                                    onSignUp(signUpUser)
                                }
                            )
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = userPasswordError != null,
                supportingText = userPasswordError?.let {
                    {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    userNameTouched = true
                    userEmailTouched = true
                    userPasswordTouched = true
                    focusManager.clearFocus()
                    if (isSignUpButtonEnabled) {
                        authViewModel.signUp(
                            userName,
                            userEmail,
                            userPassword,
                            onSignedUp = { signUpUser ->
                                onSignUp(signUpUser)
                            }
                        )
                    }
                    MixpanelManager.trackSignupCompleted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = isSignUpButtonEnabled
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Sign Up",
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
                    text = "Already have an account?",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.clickable {
                        if (!isLoading) {
                            navController.navigate(Screens.Login)
                        }
                    },
                    text = "Login",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}