package com.aritradas.medai.ui.presentation.profile

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aritradas.medai.BuildConfig
import com.aritradas.medai.R
import com.aritradas.medai.navigation.Screens
import com.aritradas.medai.ui.presentation.profile.components.SettingsCard
import com.aritradas.medai.utils.Constants
import com.aritradas.medai.utils.Resource
import com.aritradas.medai.utils.UtilsKt.getInitials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {

    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsState()
    val featureRequestState by viewModel.featureRequestState.collectAsState()
    var backPressedState by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    var featureName by remember { mutableStateOf("") }
    var featureEmail by remember { mutableStateOf("") }
    var featureDetail by remember { mutableStateOf("") }

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet && featureName.isBlank()) {
            featureName = userData?.username ?: ""
        }
    }

    LaunchedEffect(featureRequestState) {
        val currentState = featureRequestState
        when (currentState) {
            is Resource.Success -> {
                Toast.makeText(context, currentState.data, Toast.LENGTH_LONG).show()
                showBottomSheet = false
                featureName = ""
                featureEmail = ""
                featureDetail = ""
                viewModel.clearFeatureRequestState()
            }
            is Resource.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                viewModel.clearFeatureRequestState()
            }
            else -> {}
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                featureName = ""
                featureEmail = ""
                featureDetail = ""
                viewModel.clearFeatureRequestState()
            },
            sheetState = bottomSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(text = "Feature Request", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your feedback helps us improve MedAI and prioritize what matters most to users!",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                val isLoading = featureRequestState is Resource.Loading

                OutlinedTextField(
                    value = featureName,
                    onValueChange = { featureName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = featureEmail,
                    onValueChange = { featureEmail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = featureDetail,
                    onValueChange = { featureDetail = it },
                    label = { Text("Describe your feature/request") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (featureName.isBlank() || featureDetail.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please fill in all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        Toast.makeText(
                            context,
                            "Your request is being saved",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.submitFeatureRequest(featureName, featureEmail, featureDetail)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    val currentState = featureRequestState
                    when (currentState) {
                        is Resource.Loading -> Text("Submitting...")
                        else -> Text("Submit")
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = userData?.username?.let { getInitials(it) } ?: ""
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    userData?.username?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            SettingsCard(
                isFirstItem = true,
                itemName = stringResource(R.string.settings),
                itemSubText = stringResource(R.string.manage_settings_of_the_app),
                iconVector = Icons.Outlined.Settings,
                onClick = {
                    navController.navigate(Screens.Settings)
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            SettingsCard(
                isLastItem = true,
                itemName = stringResource(R.string.help),
                itemSubText = stringResource(R.string.get_help_using_medai),
                iconVector = Icons.AutoMirrored.Outlined.Help,
                onClick = {
                    navController.navigate(Screens.Help)
                }
            )


            Spacer(modifier = Modifier.height(30.dp))


            SettingsCard(
                isFirstItem = true,
                itemName = stringResource(R.string.send_love),
                itemSubText = stringResource(R.string.rate_medai_on_the_play_store),
                iconVector = Icons.Outlined.RateReview,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Constants.PLAY_STORE_URL.toUri()
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            SettingsCard(
                itemName = "Feature Request",
                itemSubText = "We'd love to hear from you!",
                iconVector = Icons.AutoMirrored.Outlined.Message,
                onClick = {
                    showBottomSheet = true
                }
            )

            Spacer(modifier = Modifier.height(2.dp))
            
            SettingsCard(
                isLastItem = true,
                itemName = stringResource(R.string.invite_friends),
                itemSubText = stringResource(R.string.like_medai_share_with_friends),
                iconVector = Icons.Outlined.Share,
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, Constants.INVITE)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )


            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Version: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                modifier = Modifier.padding(bottom = 10.dp),
                text = "Build with 💜 for people",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
