package com.aritradas.medai.ui.presentation.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aritradas.medai.R
import com.aritradas.medai.navigation.Screens
import com.aritradas.medai.ui.presentation.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    navController: NavController
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current

    var backPressedState by remember { mutableStateOf(false) }

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

    val authViewModel: AuthViewModel = hiltViewModel()
    val googleSignInResult by authViewModel.googleSignInResult.observeAsState()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.google_sign_in_failed_no_id_token),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    LaunchedEffect(googleSignInResult) {
        when (val result = googleSignInResult) {
            is com.aritradas.medai.utils.Resource.Success -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.login_successful),
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigate(Screens.Prescription) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }

            is com.aritradas.medai.utils.Resource.Error -> {
                Toast.makeText(
                    context,
                    result.message ?: "Google Sign-In failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.welcome_illustration),
                contentDescription = "Medical illustration",
                modifier = Modifier
                    .size(280.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.welcome_to_medai),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.your_smart_medical_assistant),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            FilledTonalButton(
                onClick = { 
                    val signInIntent = authViewModel.getGoogleSignInIntent(context)
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_color_icon),
                        contentDescription = "Google logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.continue_with_google),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {
                    navController.navigate(Screens.SignUp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.get_started),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}