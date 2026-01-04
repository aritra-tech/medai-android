package com.aritradas.medai.ui.presentation.prescriptionSummarize.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritradas.medai.domain.model.DrugResult

@Composable
fun DrugDetailSheetContent(detail: DrugResult) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = detail.medicineName.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        DrugDetailSection(
            title = "Uses",
            content = detail.uses,
            icon = Icons.Default.MedicalServices
        )

        Spacer(modifier = Modifier.height(10.dp))

        DrugDetailSection(
            title = "Benefits",
            content = detail.benefits,
            icon = Icons.Default.CheckCircle
        )

        Spacer(modifier = Modifier.height(10.dp))

        DrugDetailSection(
            title = "Side Effects",
            content = detail.sideEffects,
            icon = Icons.Default.Warning,
            isWarning = true
        )
    }
}