package com.aritradas.medai.ui.presentation.medicalReportSummarize

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aritradas.medai.ui.presentation.prescriptionSummarize.DrugDetailSheetContent
import com.aritradas.medai.utils.MixpanelManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedicalReportSummarizeScreen(
    navController: NavController,
    reportViewModel: MedicalReportSummarizeViewModel = hiltViewModel(),
    hasCameraPermission: Boolean = false
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val uiState by reportViewModel.uiState.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var showReportTypeDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showBackWarningDialog by remember { mutableStateOf(false) }
    var showDrugDetailModal by remember { mutableStateOf(false) }
    val drugDetail by reportViewModel.drugDetail.collectAsState()
    val isDrugLoading by reportViewModel.isDrugLoading.collectAsState()
    val drugDetailError by reportViewModel.drugDetailError.collectAsState()

    val createImageFile = {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(null)
        File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri = cameraUri
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            imageUri = uri
        }
    )

    val handleTakePhoto = {
        if (hasCameraPermission) {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraUri = photoUri
            cameraLauncher.launch(photoUri)
            showDialog = false
        } else {
            showPermissionDialog = true
            showDialog = false
        }
    }

    val handleAddImage = {
        galleryLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
        showDialog = false
    }

    val handleRemoveImage = {
        imageUri = null
        cameraUri = null
        reportViewModel.clearSummary()
    }

    val handleSummarize = {
        imageUri?.let { uri ->
            reportViewModel.validateAndAnalyzeReport(uri)
            MixpanelManager.trackMedicalReportSummarization()
        }
        Unit
    }
    reportViewModel.setOnSaveSuccessCallback {
        navController.popBackStack()
    }

    val handleReport = {
        showReportTypeDialog = true
    }
    val handleReportSubmit = {
        if (reportReason.isNotBlank()) {
            reportViewModel.updateReport(reportReason)
            reportViewModel.submitReport()
            showReportDialog = false
            showReportTypeDialog = false
            Toast.makeText(context, "Report has been submitted", Toast.LENGTH_SHORT).show()
            reportReason = ""
        }
        Unit
    }

    val handleReportTypeSelection = { reason: String ->
        if (reason == "Other") {
            showReportTypeDialog = false
            showReportDialog = true
        } else {
            reportViewModel.updateReport(reason)
            reportViewModel.submitReport()
            showReportTypeDialog = false
            Toast.makeText(context, "Report has been submitted", Toast.LENGTH_SHORT).show()
        }
    }

    val handleBackNavigation = {
        val hasOngoingProcess = uiState.isValidating || uiState.isLoading
        val hasSummary = uiState.summary != null

        if (hasOngoingProcess || hasSummary) {
            showBackWarningDialog = true
        } else {
            navController.popBackStack()
        }
        Unit
    }

    if (showDrugDetailModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showDrugDetailModal = false
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                when {
                    isDrugLoading -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoadingIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    drugDetail != null -> {
                        DrugDetailSheetContent(detail = drugDetail!!)
                    }

                    drugDetailError != null -> {
                        Text(
                            text = drugDetailError ?: "No data.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Medical Report",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackNavigation) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    uiState.summary?.let {
                        IconButton(
                            onClick = {
                                reportViewModel.saveMedicalReport()
                                MixpanelManager.savedMedicalReport()
                            },
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Save report",
                                    tint = if (uiState.saveSuccess)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 2.dp.toPx()
                    val dashLength = 10.dp.toPx()
                    val gapLength = 8.dp.toPx()

                    drawRoundRect(
                        color = Color.Gray,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dashLength, gapLength)
                            )
                        ),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }

                if (imageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected report image",
                            modifier = Modifier
                                .width(240.dp)
                                .height(240.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )

                        FloatingActionButton(
                            onClick = handleRemoveImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp),
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Upload a medical report",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Take a photo of your report or upload an existing image",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text(
                                text = "Upload",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = handleSummarize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = imageUri != null && !uiState.isLoading && !uiState.isValidating
            ) {
                when {
                    uiState.isValidating -> {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Verifying",
                                color = Color.White
                            )
                        }
                    }

                    uiState.isLoading -> {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Analyzing",
                                color = Color.White
                            )
                        }
                    }

                    else -> {
                        Text("Summarize")
                    }
                }
            }

            uiState.summary?.let { summary ->
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ AI-generated content - verify with doctor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = handleReport,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report content",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Report Summary",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (summary.summary.isNotEmpty()) {
                            Text(
                                text = summary.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }


                        if (summary.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Important Points:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    summary.warnings.forEach { warning ->
                                        Text(
                                            text = "• $warning",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    uiState.saveError?.let { error ->
        AlertDialog(
            onDismissRequest = { reportViewModel.clearSaveStatus() },
            title = { Text("Save Failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { reportViewModel.clearSaveStatus() }) {
                    Text("OK")
                }
            }
        )
    }

    uiState.validationError?.let { error ->
        AlertDialog(
            onDismissRequest = { reportViewModel.clearValidationError() },
            title = { Text("Invalid Report") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { reportViewModel.clearValidationError() }) {
                    Text("OK")
                }
            }
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { reportViewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { reportViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = {
                Column {
                    TextButton(
                        onClick = handleTakePhoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Take Photo")
                        }
                    }

                    TextButton(
                        onClick = handleAddImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Add Image"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add Image")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text("Permission Required")
            },
            text = {
                Text("This app needs camera permission to take photos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showPermissionDialog = false
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReportTypeDialog) {
        Dialog(
            onDismissRequest = { showReportTypeDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Select Report Type",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                handleReportTypeSelection("Medical Inaccuracy")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = false,
                            onClick = {
                                handleReportTypeSelection("Medical Inaccuracy")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Medical Inaccuracy")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                handleReportTypeSelection("Misinformation")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = false,
                            onClick = {
                                handleReportTypeSelection("Misinformation")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Misinformation")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                handleReportTypeSelection("Other")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = false,
                            onClick = {
                                handleReportTypeSelection("Other")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Other")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReportTypeDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Content") },
            text = {
                Column {
                    Text("Is this content problematic or incorrect? Please explain below.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Report Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = handleReportSubmit) {
                    Text("Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackWarningDialog) {
        AlertDialog(
            onDismissRequest = { showBackWarningDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("There are unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Okay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}